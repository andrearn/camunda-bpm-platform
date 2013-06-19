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

package org.camunda.bpm.engine.impl.pvm.runtime;

import java.util.Map;

import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;


/**
 * @author Daniel Meyer
 */
public class ExecutionStartContext {
  
  protected final ActivityImpl selectedInitial;
  protected Map<String, Object> variables;

  public ExecutionStartContext(ActivityImpl selectedInitial) {
    this.selectedInitial = selectedInitial;
  }

  public ActivityImpl getInitial() {
    return selectedInitial;
  }

  public void setVariables(Map<String, Object> variables) {
    this.variables = variables;
  }
  
  public Map<String, Object> getVariables() {
    return variables;
  }

  public void initialStarted(InterpretableExecution execution) {
    // set variables in the context of the activity instance of the selected initial (start event)
    if(variables != null) {
      execution.setVariables(variables);
    }
  }
  
}