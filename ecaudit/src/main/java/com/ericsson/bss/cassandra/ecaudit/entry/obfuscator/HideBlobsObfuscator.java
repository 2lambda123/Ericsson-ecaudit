/*
 * Copyright 2019 Telefonaktiebolaget LM Ericsson
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
package com.ericsson.bss.cassandra.ecaudit.entry.obfuscator;

import java.nio.ByteBuffer;
import java.util.Optional;

import org.apache.cassandra.cql3.CQL3Type;
import org.apache.cassandra.cql3.ColumnSpecification;
import org.apache.cassandra.db.marshal.AbstractType;

public class HideBlobsObfuscator implements ColumnObfuscator
{
    @Override
    public Optional<String> obfuscate(ColumnSpecification column, ByteBuffer value)
    {
        return isBlobType(column.type) || isCollectionContainingBlobType(column.type)
               ? Optional.of("<" + column.type.asCQL3Type() + ">")
               : Optional.empty();
    }

    private static boolean isBlobType(AbstractType<?> type)
    {
        return type.asCQL3Type() == CQL3Type.Native.BLOB;
    }

    private static boolean isCollectionContainingBlobType(AbstractType<?> type)
    {
        return type.isCollection() && type.asCQL3Type().toString().contains("blob");
    }
}
