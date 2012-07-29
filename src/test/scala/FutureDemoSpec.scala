import concurrent.forkjoin.ForkJoinPool
import concurrent.{ExecutionContext, Future}
import concurrent.future
import java.util.concurrent.{TimeUnit, ExecutionException}
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import java.util.concurrent.TimeUnit.{MILLISECONDS => MS}


class FutureDemoSpec extends FunSuite with ShouldMatchers {


  //NOTE: the tests don't fail in expected ways  see note on assertions below

  implicit val executionContext = ExecutionContext.fromExecutorService(new ForkJoinPool(10))

  test("demo callbacks") {
    val f = future {
      5
    }
    val g = future {
      3
    }
    val b: Future[Int] = future {
      throw new AssertionError("oho!")
    }

    val h = for {
      x <- f // returns Future(5)
      y <- g // returns Future(3)
    } yield x + y

    h onSuccess {
      /**
       * Note on assertions
       * changing this value to try and fail a test will not work as expected
       * under sbt the test will appear green but look for a exception that has the line number in its stacktrace
       */
      case x: Int => x should be(8)
      case y => fail("unexpected " + y)
    }

    val l = for {
      x <- f // returns Future(5)
      y <- g // returns Future(3)
      z <- b // returns Future(AssertionError oho)
    } yield x + y + z

    l onFailure {
      case x: ExecutionException => x.getCause.getClass should be(classOf[AssertionError])
      case y => fail("unexpected " + y)
    }
    val to = future {
      TimeUnit.SECONDS.sleep(2); Math.PI
    }
    val tol = for {
      x <- f // returns Future(5)
      y <- g // returns Future(3)
      z <- to
    } yield x + y + z

    tol onComplete {
      _.fold(t => fail("unexpected " + t), i => i should be(5 + 3 + Math.PI))
    }
  }

  test("demo sequence") {
    val f = future {
      5
    }
    val g = future {
      3
    }
    val h = future {
      2
    }

    val sequence: Future[Vector[Int]] = Future.sequence(Vector(f, g, h))

    for (i <- sequence) (i.sum should be(10))
  }

  test("demo traverse") {
    val values: Vector[Int] = Vector(1, 2, 3)
    val traverse: Future[Vector[Int]] = Future.traverse(values)((i: Int) => future(i * 2))

    for (i <- traverse) (i.sum should be(12))
  }

  test("dem creating your own abstraction") {

    /**
     * Waits till a certain block has executed (usually a timer) then collects all the futures that have completed.
     *
     * These subset of futures can then be retrieved using gather
     *
     * Note: Completed doesn't imply success.
     *
     * The following code example shows how to wait for roughly 100 ms and then gather the results
     * <code>
     * Futurez(Future{...}, Future{...}, Future{...}).till {
     *   java.util.concurrent.TimeUnit.MILLISECONDS.sleep(100)
     * }.gather
     * </code>
     *
     * This is an excellent construct for satisfying SLAs.
     *
     * Note: The till block need not be an obvious timer, it can be any computation of any duration. One usage would be to have an activity whos success determines the overall outcome! (though this aspect could be brought out with a better api design of Futurez)
     *
     * @param fs futures to execute
     */
    case class Futurez[T](fs: Future[T]*) {
      private var collected: List[Future[T]] = List()

      def till(w: => Unit): Futurez[T] = {
        //println("till 1")
        val s = w
        //println("till 2")
        collected = fs.filter(_.isCompleted).toList
        //println("collected " + collected mkString)
        this
      }

      def gather: List[Future[T]] = {
        //println("gather")
        collected
      }
    }

    val fz = Futurez(future {
      MS sleep 10
      10
    }, future {
      MS sleep 20
      40
    }, future {
      MS sleep 30
      20
    }, future {
      MS sleep 45
      30
    }).till(MS sleep 39)

    val gather: List[Future[Int]] = fz.gather

    fz.gather.size should be(3)
    for (i <- Future.sequence(gather)) (i.sum should be(70))
  }



}
