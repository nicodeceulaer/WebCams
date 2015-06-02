/*
* ******************************************************************************
* Copyright (c) 2013-2015 Tomas Valenta.
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
* *****************************************************************************
*/

package cz.yetanotherview.webcamviewer.app.analyzer;

import java.util.List;

import cz.yetanotherview.webcamviewer.app.model.Link;

public interface AnalyzerCallback {
    void onAnalyzingUpdate(String message);
    void onAnalyzingFailed(List<Link> links, String Url, int errorCode);
    void onAnalyzingCompleted(List<Link> links, boolean fromComplete);
}