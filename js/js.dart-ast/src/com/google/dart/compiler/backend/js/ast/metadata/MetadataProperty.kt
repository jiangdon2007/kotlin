/*
 * Copyright 2010-2014 JetBrains s.r.o.
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
 */

package com.google.dart.compiler.backend.js.ast.metadata

internal class MetadataProperty<in T : HasMetadata, R>(val default: R) {
    operator fun getValue(thisRef: T, desc: PropertyMetadata): R {
        if (!thisRef.hasData(desc.name)) return default
        return thisRef.getData<R>(desc.name)
    }

    operator fun setValue(thisRef: T, desc: PropertyMetadata, value: R) {
        thisRef.setData(desc.name, value)
    }
}
