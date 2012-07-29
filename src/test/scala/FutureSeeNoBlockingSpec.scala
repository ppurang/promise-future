import concurrent.forkjoin.ForkJoinPool
import concurrent.util.FiniteDuration
import concurrent._
import java.util.concurrent.{TimeUnit, ExecutionException}
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import java.util.concurrent.TimeUnit.{MILLISECONDS => MS}

class FutureSeeNoBlockingSpec extends FunSuite with ShouldMatchers {

  implicit val executionContext = ExecutionContext.fromExecutorService(new ForkJoinPool(10))

  test("how not to block: need a house") {
    import CUtils.DebugUtils._
    //A lender that lends money given someone agrees to pay a fixed interest equal to some percentage
    case class LenderX(percentage: Int) {

      def isValid(need: Int, promised: Int): Boolean = need + (need * percentage / 100) == promised

      //lending money only works if a promise to return money is provided
      def lend(moneyNeeded: Int)(promiseToPayInTheFuture: (Int, Promise[Int])) = f {
        if (isValid(moneyNeeded, promiseToPayInTheFuture._1)) {
          doSomePaperWork(promiseToPayInTheFuture._1, promiseToPayInTheFuture._2)
          moneyNeeded
        } else throw new AssertionError("read the small print dude!")
      } {
        "lending money"
      }


      def doSomePaperWork(required: Int, p: Promise[Int]) = {
        p.future.andThen {
          //create a reminder to check when the promise is fulfilled
          case Right(v) => if (v >= required) {
            tprintln("thanks") //some pleasantries
          } else {
            tprintln("fraud " + v) //raise alarm
          }

          case Left(t) => tprintln("default") //this would be some type of alarm too
        }
        TimeUnit.MILLISECONDS.sleep(1000)
      }
    }

    val moneyLender = LenderX(10) //lender lending money if someone agrees on the 10% interest rate


    case class House(i: Int) //some commodity to buy

    // buys a dream house on the market for the given amount
    def buy(amount: Int): House = {
      tprintln("buying house")
      House(amount)
    }

    // sells a house on the market where the market always leads to a 25% appreciation in value
    def sell(h: House): Int = {
      tprintln("selling house")
      h.i + h.i * 25 / 100
    }

    val iPromise110Euros = (110, Promise[Int]()) // a promise for 110

    //iPromise110Euros._2 completeWith ((moneyLender.lend(100)(iPromise110Euros).map {buy} map {sell})
    iPromise110Euros._2 completeWith (moneyLender.lend(100)(iPromise110Euros).map {
      tprintln("buying house")
      House(_)
    } map {
      tprintln("selling house")
      sell
    })

  }

  test("how not to block: profit or Ka-ching!") {
    import CUtils.DebugUtils._
    //A lender that lends money given someone agrees to pay a fixed interest equal to some percentage
    case class LenderX(percentage: Int) {

      def isValid(need: Int, promised: Int): Boolean = need + (need * percentage / 100) == promised

      //lending money only works if a promise to return money is provided
      def lend(moneyNeeded: Int)(promiseToPayInTheFuture: (Int, Promise[Int])) = f {
        if (isValid(moneyNeeded, promiseToPayInTheFuture._1)) {
          doSomePaperWork(promiseToPayInTheFuture._1, promiseToPayInTheFuture._2)
          moneyNeeded
        } else throw new AssertionError("read the small print dude!")
      } {
        "lending money"
      }


      def doSomePaperWork(required: Int, p: Promise[Int]) = {
        p.future.andThen {
          //create a reminder to check when the promise is fulfilled
          case Right(v) => if (v >= required) {
            tprintln("thanks") //some pleasantries
          } else {
            tprintln("fraud " + v) //raise alarm
          }

          case Left(t) => tprintln("default") //this would be some type of alarm too
        }
        TimeUnit.MILLISECONDS.sleep(1000)
      }
    }

    val moneyLender = LenderX(10) //lender lending money if someone agrees on the 10% interest rate


    case class House(i: Int) //some commodity to buy

    // buys a dream house on the market for the given amount
    def buy(amount: Int): House = {
      tprintln("buying house")
      House(amount)
    }

    // sells a house on the market where the market always leads to a 25% appreciation in value
    def sell(h: House): Int = {
      tprintln("selling house")
      h.i + h.i * 25 / 100
    }

    val iPromise110Euros = (110, Promise[Int]()) // a promise for 110

    //another way
    val makeSomeMoneyInTheFuture: Future[Int] = moneyLender.lend(100)(iPromise110Euros).map {
      House(_)
    } map {
      sell
    }

    iPromise110Euros._2 completeWith makeSomeMoneyInTheFuture

    val profit = Await.result(makeSomeMoneyInTheFuture map (_ - iPromise110Euros._1), FiniteDuration(10000, "ms"))
    profit should be(15)
  }

}
