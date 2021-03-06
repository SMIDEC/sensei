/**
 * This software is licensed to you under the Apache License, Version 2.0 (the
 * "Apache License").
 *
 * LinkedIn's contributions are made under the Apache License. If you contribute
 * to the Software, the contributions will be deemed to have been made under the
 * Apache License, unless you expressly indicate otherwise. Please do not make any
 * contributions that would be inconsistent with the Apache License.
 *
 * You may obtain a copy of the Apache License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, this software
 * distributed under the Apache License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Apache
 * License for the specific language governing permissions and limitations for the
 * software governed under the Apache License.
 *
 * © 2012 LinkedIn Corp. All Rights Reserved.  
 */
package com.senseidb.search.req.mapred.functions;

import java.io.Serializable;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.senseidb.search.req.mapred.CombinerStage;
import com.senseidb.search.req.mapred.FacetCountAccessor;
import com.senseidb.search.req.mapred.FieldAccessor;
import com.senseidb.search.req.mapred.SenseiMapReduce;

public class MinMapReduce implements SenseiMapReduce<MinResult, MinResult> {

  private String column;

  @Override
  public MinResult map(int[] docIds, int docIdCount, long[] uids, FieldAccessor accessor, FacetCountAccessor facetCountAccessor) {
    double min = Double.MAX_VALUE;
    double tmp = 0;
    long uid = 0l;
    for (int i =0; i < docIdCount; i++) {
      tmp = accessor.getDouble(column, docIds[i]);
      if (min > tmp) {       
        min = tmp;
        uid = uids[docIds[i]];
      }
    }
    return new MinResult(min, uid);
  }

  @Override
  public List<MinResult> combine(List<MinResult> mapResults, CombinerStage combinerStage) {
    if (mapResults.isEmpty()) {
      return mapResults;
    }
    MinResult ret = mapResults.get(0);
    for (int i = 1; i < mapResults.size(); i++) {
      if (ret.value > mapResults.get(i).value) {
        ret = mapResults.get(i);
      }
    }
    mapResults.clear();
    mapResults.add(ret);
    return mapResults;
  }

  @Override
  public MinResult reduce(List<MinResult> combineResults) {
    if (combineResults.isEmpty()) {
      return null;
    }
    MinResult ret = combineResults.get(0);
    for (int i = 1; i < combineResults.size(); i++) {
      if (ret.value > combineResults.get(i).value) {
        ret = combineResults.get(i);
      }
    }
    return ret;
  }

  @Override
  public JSONObject render(MinResult reduceResult) {
    
    try {
      return new JSONObject().put("min", reduceResult.value).put("uid", reduceResult.uid);
    } catch (JSONException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void init(JSONObject params) {
     column = params.optString("column");
    if (column == null) {
      throw new IllegalStateException("Column parameter shouldn't be null");
    }
  }
 
}
class MinResult implements Serializable {
  public double value;
  public long uid;
  public MinResult(double value, long uid) {
    super();
    this.value = value;
    this.uid = uid;
  }
  
}
