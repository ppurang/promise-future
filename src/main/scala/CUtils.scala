/**
 * Created with IntelliJ IDEA.
 * User: ppurang
 * Date: 7/23/12
 * Time: 8:40 PM
 * To change this template use File | Settings | File Templates.
 */

import concurrent._

import ExecutionContext.Implicits.global

object CUtils {

  object DebugUtils {
    /**
     * print some debug info with info about the thread running the code
     */
    def tprintln(a: Any, c: String = "") = println("%s (%d) %s: %s".format(Thread.currentThread().getName, Thread.currentThread().getId, c, a))

    /**
     * same as above except it self run in another thread (will be an independent thread so the utility is limited)
     */
    def ftprintln(a: Any) = future {
      tprintln(a)
    }

    /**
     * executes a block (b) and then prints some debug info including the thread info on thread running the block of code
     */
    def f[T](b: => T)(implicit context: String = "unknown") = future {
      val x = b
      tprintln(x, context)
      x
    }

  }



}
