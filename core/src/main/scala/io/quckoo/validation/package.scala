package io.quckoo

import scalaz._
import Scalaz._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 21/10/2016.
  */
package object validation {
  type ValidatorK[F[_], A] = Kleisli[F, A, ValidationNel[Violation, A]]
  type Validator[A] = ValidatorK[Id, A]

  object Validator {
    def apply[F[_]: Functor, A](test: A => F[Boolean], err: A => Violation): ValidatorK[F, A] = Kleisli { a =>
      test(a).map(cond => if (cond) a.successNel[Violation] else err(a).failureNel[A])
    }
  }

  object conjunction {
    implicit def semigroup[F[_]: Applicative]: Plus[ValidatorK[F, ?]] = new Plus[ValidatorK[F, ?]] {
      override def plus[A](a: ValidatorK[F, A], b: => ValidatorK[F, A]): ValidatorK[F, A] = Kleisli { x =>
        (a.run(x) |@| b.run(x)) {
          case (l, r) => (l |@| r)((_, out) => out)
        }
      }
    }
  }

  object disjunction {
    implicit def semigroup[F[_]: Applicative]: Plus[ValidatorK[F, ?]] = new Plus[ValidatorK[F, ?]] {
      override def plus[A](a: ValidatorK[F, A], b: => ValidatorK[F, A]): ValidatorK[F, A] = Kleisli { x =>
        (a.run(x) |@| b.run(x)) {
          case (l, r) => l.orElse(r)
        }
      }
    }
  }

  implicit class ValidatorOps[A](self: Validator[A]) {
    def async(implicit ec: ExecutionContext): ValidatorK[Future, A] = self.lift[Future]
  }

  implicit class ValidatorKOps[F[_]: Applicative, A](self: ValidatorK[F, A]) {
    def and(other: ValidatorK[F, A]): ValidatorK[F, A] =
      conjunction.semigroup[F].plus(self, other)
    def &&(other: ValidatorK[F, A]): ValidatorK[F, A] = and(other)

    def or(other: ValidatorK[F, A]): ValidatorK[F, A] =
      disjunction.semigroup[F].plus(self, other)
    def ||(other: ValidatorK[F, A]): ValidatorK[F, A] = or(other)

    def product[B](other: ValidatorK[F, B]): ValidatorK[F, (A, B)] = Kleisli { case (a, b) =>
      (self.run(a) |@| other.run(b))((l, r) => (l |@| r)(_ -> _))
    }
    def *[B](other: ValidatorK[F, B]): ValidatorK[F, (A, B)] = product(other)
  }

  implicit class ValidatorK2Ops[F[_]: Applicative, A, B](self: ValidatorK[F, (A, B)]) {
    def product[C](other: ValidatorK[F, C]): ValidatorK[F, (A, B, C)] = Kleisli { case (a, b, c) =>
      (self.run((a, b)) |@| other.run(c))((l, r) => (l |@| r) { case ((a1, b1), c1) => (a1, b1, c1) })
    }
    def *[C](other: ValidatorK[F, C]): ValidatorK[F, (A, B, C)] = product(other)
  }
}
