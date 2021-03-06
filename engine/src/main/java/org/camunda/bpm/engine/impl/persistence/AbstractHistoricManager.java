/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.camunda.bpm.engine.impl.persistence;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.context.Context;


/**
 * @author Tom Baeyens
 */
public class AbstractHistoricManager extends AbstractManager {

  protected int historyLevel = Context.getProcessEngineConfiguration().getHistoryLevel();
  protected boolean isHistoryEnabled = historyLevel > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE;
  protected boolean isHistoryLevelFullEnabled = historyLevel >= ProcessEngineConfigurationImpl.HISTORYLEVEL_FULL;

  protected void checkHistoryEnabled() {
    if (!isHistoryEnabled) {
      throw new ProcessEngineException("history is not enabled");
    }
  }

  public boolean isHistoryEnabled() {
    return isHistoryEnabled;
  }

  public boolean isHistoryLevelFullEnabled() {
    return isHistoryLevelFullEnabled;
  }
}
