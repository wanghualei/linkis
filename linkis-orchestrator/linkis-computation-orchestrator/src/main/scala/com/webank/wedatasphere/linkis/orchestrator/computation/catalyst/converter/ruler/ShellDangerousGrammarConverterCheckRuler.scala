/*
 * Copyright 2019 WeBank
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.webank.wedatasphere.linkis.orchestrator.computation.catalyst.converter.ruler

import com.webank.wedatasphere.linkis.common.utils.Logging
import com.webank.wedatasphere.linkis.orchestrator.computation.catalyst.converter.exception.SensitiveTablesCheckException
import com.webank.wedatasphere.linkis.orchestrator.computation.conf.ComputationOrchestratorConf
import com.webank.wedatasphere.linkis.orchestrator.computation.entity.ComputationJobReq
import com.webank.wedatasphere.linkis.orchestrator.domain.JobReq
import com.webank.wedatasphere.linkis.orchestrator.extensions.catalyst.ConverterCheckRuler
import com.webank.wedatasphere.linkis.orchestrator.plans.ast.ASTContext


class ShellDangerousGrammarConverterCheckRuler extends ConverterCheckRuler  with Logging {

  private val shellDangerUsage = ComputationOrchestratorConf.SHELL_DANGER_USAGE.getValue
  info(s"SHELL DANGER USAGE ${shellDangerUsage}")

  private val shellWhiteUsage = ComputationOrchestratorConf.SHELL_WHITE_USAGE.getValue
  info(s"SHELL White USAGE ${shellWhiteUsage}")

  private val shellWhiteUsageEnabled = ComputationOrchestratorConf.SHELL_WHITE_USAGE_ENABLED.getValue
  info(s"Only Allow SHELL White USAGE? ${shellWhiteUsage}")


  def shellWhiteUsage(shellContent:String): Boolean = {
    if(!shellWhiteUsageEnabled) return true
    val shellLines = shellContent.split("\n")
    var signature: Boolean = false
    shellLines foreach {
      shellLine =>
        val shellCommand: String = shellLine.trim.split(" ")(0)
        if (shellWhiteUsage.split(",").contains(shellCommand)){
          signature = true
        }
    }
    signature
  }

  def shellContainDangerUsage(shellContent:String): Boolean ={
    val shellLines = shellContent.split("\n")
    var signature:Boolean = false
    shellLines synchronized{
      shellLines foreach{
        shellLine =>
          if (shellLine.trim.endsWith(".sh")){//????????????shell??????
            signature=true
          }else{
            val shellCommands = shellLine.trim.split(" ")
            shellCommands foreach{
              shellCommand =>  shellDangerUsage.split(",").contains(shellCommand) match {
                case true => signature=true
                case _ =>
              }
            }
          }
      }
    }
    signature
  }

  /**
    * The apply function is to supplement the information of the incoming parameter task, making the content of this task more complete.
    * ????* Additional information includes: database information supplement, custom variable substitution, code check, limit limit, etc.
    * apply????????????????????????task????????????????????????????????????task????????????????????????
    * ?????????????????????: ???????????????????????????????????????????????????????????????limit?????????
    *
    * @param in
    * @return
    */
  override def apply(in: JobReq, context: ASTContext): Unit = {
    in match {
      case computationJobReq: ComputationJobReq =>
        if ("shell".equals(computationJobReq.getCodeLanguageLabel.getCodeType)
          || "sh".equals(computationJobReq.getCodeLanguageLabel.getCodeType)){
          info(s"GET REQUEST RUNTYPE ${computationJobReq.getCodeLanguageLabel.getCodeType}")
          if (/**PythonMySqlUtils.checkSensitiveTables(entranceJobReq.getExecutionContent) && **/!shellContainDangerUsage(computationJobReq.getCodeLogicalUnit.toStringCode) && shellWhiteUsage(computationJobReq.getCodeLogicalUnit.toStringCode)) {
            //info(s"CHECK SENSITIVE table ${PythonMySqlUtils.checkSensitiveTables(entranceJobReq.getExecutionContent)}")
            info(s"CHECK SENSITIVE code ${!shellContainDangerUsage(computationJobReq.getCodeLogicalUnit.toStringCode)}")
          }
          else throw SensitiveTablesCheckException("???????????????????????????????????????????????????")
        }
      case _ =>
    }
  }

  override def getName: String = "ShellDangerousGrammarConverterCheckRuler"
}
