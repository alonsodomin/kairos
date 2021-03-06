/*
 * Copyright 2015 A. Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.worker.executor

import java.util.UUID

import akka.testkit._

import io.quckoo.{ShellScriptPackage, TaskExitCodeFault, TaskId}
import io.quckoo.testkit.QuckooActorSuite
import io.quckoo.worker.core.{TaskExecutor, WorkerContext}

import org.scalamock.scalatest.MockFactory

class ShellTaskExecutorSpec
    extends QuckooActorSuite("ShellTaskExecutorSpec") with ImplicitSender with MockFactory {

  "ShellTaskExecutor" should {
    "run shell scripts and capture its standard output" in {
      val expectedOut   = "Hello World!"
      val script        = s"""echo "$expectedOut""""
      val workerContext = mock[WorkerContext]

      val taskId        = TaskId(UUID.randomUUID())
      val scriptPackage = ShellScriptPackage(script)

      val executor =
        TestActorRef(ShellTaskExecutor.props(workerContext, taskId, scriptPackage), "hello-bash")

      executor ! TaskExecutor.Run

      val completed = expectMsgType[TaskExecutor.Completed]
      completed.result shouldBe s"$expectedOut\n"
    }

    "reply with a failure if script exit code is not 0" in {
      val expectedExitCode = 123
      val script           = s"""
      | #!/bin/bash
      | exit $expectedExitCode
      """.stripMargin

      val workerContext = mock[WorkerContext]
      val taskId        = TaskId(UUID.randomUUID())
      val scriptPackage = ShellScriptPackage(script)

      val executor =
        TestActorRef(ShellTaskExecutor.props(workerContext, taskId, scriptPackage), "bash-exitcode")

      executor ! TaskExecutor.Run

      val failedMsg = expectMsgType[TaskExecutor.Failed]
      failedMsg.error shouldBe TaskExitCodeFault(expectedExitCode)
    }
  }

}
