package com.nokia.mesos.impl.launcher

import monix.execution.Ack
import monix.execution.Ack.Stop
import monix.reactive.Observable.Operator
import monix.reactive.observers.Subscriber

import scala.concurrent.Future
import scala.util.control.NonFatal

final class TakeUntilByPredicateOperator[A](p: A => Boolean)
    extends Operator[A, A] {

  def apply(out: Subscriber[A]): Subscriber[A] =
    new Subscriber[A] {
      implicit val scheduler = out.scheduler
      private[this] var isActive = true

      def onNext(elem: A): Future[Ack] = {
        if (!isActive) Stop
        else {
          // Protects calls to user code from within an operator
          var streamError = true
          try {
            val isLast = p(elem)
            streamError = false

            if (!isLast) {
              out.onNext(elem)
            } else {
              isActive = false
              out.onNext(elem)
              out.onComplete()
              Stop
            }
          } catch {
            case NonFatal(ex) if streamError =>
              onError(ex)
              Stop
          }
        }
      }

      def onComplete() =
        if (isActive) {
          isActive = false
          out.onComplete()
        }

      def onError(ex: Throwable) =
        if (isActive) {
          isActive = false
          out.onError(ex)
        }
    }
}
