/*
 * Copyright (c) 2018-present, easy-4-java (https://github.com/easy-4-java).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package io.github.easy4j.kimi;

/**
 * Base unchecked exception raised by kimi-java-sdk.
 *
 * <p>Existing constructors retain the historical GENERAL category so callers
 * that already catch {@code KimiException} remain source compatible. New
 * specialized exceptions expose a stable {@link KimiErrorCategory}.</p>
 */
public class KimiException extends RuntimeException {

    private final KimiErrorCategory category;

    public KimiException(String message) {
        this(KimiErrorCategory.GENERAL, message, null);
    }

    public KimiException(String message, Throwable cause) {
        this(KimiErrorCategory.GENERAL, message, cause);
    }

    protected KimiException(KimiErrorCategory category, String message) {
        this(category, message, null);
    }

    protected KimiException(KimiErrorCategory category, String message, Throwable cause) {
        super(message, cause);
        this.category = category == null ? KimiErrorCategory.GENERAL : category;
    }

    public KimiErrorCategory getCategory() {
        return category;
    }
}
