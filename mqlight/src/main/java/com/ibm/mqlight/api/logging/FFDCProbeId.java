/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.ibm.mqlight.api.logging;

/**
 * Probe identifiers for First Failure Data Capture.
 */
public enum FFDCProbeId {
  PROBE_001("FFDC_001"),
  PROBE_002("FFDC_002"),
  PROBE_003("FFDC_003"),
  PROBE_004("FFDC_004"),
  PROBE_005("FFDC_005"),
  PROBE_006("FFDC_006"),
  PROBE_007("FFDC_007"),
  PROBE_008("FFDC_008"),
  PROBE_009("FFDC_009"),
  PROBE_010("FFDC_010"),
  PROBE_011("FFDC_011"),
  PROBE_012("FFDC_012"),
  PROBE_013("FFDC_013"),
  PROBE_014("FFDC_014"),
  PROBE_015("FFDC_015"),
  PROBE_016("FFDC_016"),
  PROBE_017("FFDC_017"),
  PROBE_018("FFDC_018"),
  PROBE_019("FFDC_019"),
  PROBE_020("FFDC_020"),
  PROBE_021("FFDC_021"),
  PROBE_022("FFDC_022"),
  PROBE_023("FFDC_023"),
  PROBE_024("FFDC_024"),
  PROBE_025("FFDC_025"),
  PROBE_026("FFDC_026"),
  PROBE_027("FFDC_027"),
  PROBE_028("FFDC_028"),
  PROBE_029("FFDC_029"),
  PROBE_030("FFDC_030"),
  PROBE_031("FFDC_031"),
  PROBE_032("FFDC_032"),
  PROBE_033("FFDC_033"),
  PROBE_034("FFDC_034"),
  PROBE_035("FFDC_035"),
  PROBE_036("FFDC_036"),
  PROBE_037("FFDC_037"),
  PROBE_038("FFDC_038"),
  PROBE_039("FFDC_039"),
  PROBE_040("FFDC_040"),
  PROBE_041("FFDC_041"),
  PROBE_042("FFDC_042"),
  PROBE_043("FFDC_043"),
  PROBE_044("FFDC_044"),
  PROBE_045("FFDC_045"),
  PROBE_046("FFDC_046"),
  PROBE_047("FFDC_047"),
  PROBE_048("FFDC_048"),
  PROBE_049("FFDC_049");

  private final String id;
  
  private FFDCProbeId(String id) {
    this.id = id;
  }
  
  public String toString() {
    return id;
  }
}
