import collection.mutable.ArrayBuffer
import concurrent.forkjoin.ForkJoinPool
import concurrent.util.{FiniteDuration, Duration}
import concurrent.{ExecutionContext, Await, Future}
import concurrent.{future}
import java.util.concurrent.{TimeUnit, TimeoutException, ExecutionException}
import java.util.concurrent.TimeUnit._
import org.scalatest.{FunSuite, FunSpec}
import org.scalatest.matchers.ShouldMatchers
import java.util.concurrent.TimeUnit.{MILLISECONDS => MS}
import util.Try


class FutureForComprehensionsSpec extends FunSuite with ShouldMatchers {

  import CUtils.DebugUtils._

  implicit val executionContext = ExecutionContext.fromExecutorService(new ForkJoinPool(10))

  test("For comprehension: first future explodes slowly") {
    val k: Future[Int] = f {
      MS sleep 1000; throw new AssertionError("slowly")
    }
    val g = f {
      MS sleep 100; 10
    }

    val h = for {
      x <- k
      y <- g
    } yield x + y

    intercept[TimeoutException] {
      Await.result(h, FiniteDuration(900, "ms"))
    }

    intercept[ExecutionException] {
      Await.result(h, FiniteDuration(1100, "ms"))
    }

  }
  test("For comprehension: first future explodes fast") {
    val k: Future[Int] = f {
      MS sleep 100; println("going to explode"); throw new AssertionError("fast")
    }
    val g = f {
      MS sleep 1000; 20
    }


    val h = for {
      x <- k
      y <- g
    } yield x + y

    intercept[ExecutionException] {
      Await.result(h, FiniteDuration(200, "ms"))
    }
  }
  test("For comprehension: future explodes fast but is placed after a rather slower one") {
    val k: Future[Int] = f {
      MS.sleep(300); println("going to explode"); throw new AssertionError("300")
    }
    val g = f {
      MS.sleep(3000); 25
    }
    val s = f {
      MS.sleep(1000); 35
    }

    val h = for {
      y <- g
      x <- k
      z <- s
    } yield x + y + z

    intercept[TimeoutException] {
      Await.result(h, FiniteDuration(300, "ms"))
    }

    intercept[TimeoutException] {
      Await.result(h, FiniteDuration(2000, "ms"))
    }

    intercept[ExecutionException] {
      Await.result(h, FiniteDuration(800, "ms")) //600 would fail
    }

  }


  test("For comprehension: comparable to map flatmap combo") {
    val g = f {
      10
    }
    val s = f {
      15
    }

    val h = for {
      y <- g
      x <- s
    } yield x + y


    val h2 = g map (y => s map (x => x + y)) flatMap identity

    Await.result(h, FiniteDuration(1, "ms")) should be(Await.result(h2, FiniteDuration(1, "ms")))

  }


}
