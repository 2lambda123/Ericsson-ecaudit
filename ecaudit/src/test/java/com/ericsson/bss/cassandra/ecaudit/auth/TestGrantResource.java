/*
 * Copyright 2020 Telefonaktiebolaget LM Ericsson
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
package com.ericsson.bss.cassandra.ecaudit.auth;

import org.junit.Test;

import org.apache.cassandra.auth.DataResource;
import org.apache.cassandra.auth.Permission;
import org.apache.cassandra.auth.RoleResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class TestGrantResource
{
    @Test
    public void testRootLevelGrant()
    {
        GrantResource root = GrantResource.root();

        assertThat(root.getName()).isEqualTo("grants");
        assertThat(root.exists()).isTrue();
        assertThat(root.hasParent()).isFalse();
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(root::getParent);
        assertThat(root.applicablePermissions()).isEqualTo(Permission.ALL);
    }

    @Test
    public void testDataGrant()
    {
        DataResource dataResource = DataResource.fromName("data/ks/tbl");
        GrantResource resource = GrantResource.fromResource(dataResource);

        assertThat(resource.getName()).isEqualTo("grants/data/ks/tbl");
        assertThat(resource.exists()).isTrue();
        assertThat(resource.hasParent()).isFalse();
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(resource::getParent);
        assertThat(resource.applicablePermissions()).isEqualTo(dataResource.applicablePermissions());
    }

    @Test
    public void testRolesGrant()
    {
        RoleResource roleResource = RoleResource.fromName("roles/kalle");
        GrantResource resource = GrantResource.fromResource(roleResource);

        assertThat(resource.getName()).isEqualTo("grants/roles/kalle");
        assertThat(resource.exists()).isTrue();
        assertThat(resource.hasParent()).isFalse();
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(resource::getParent);
        assertThat(resource.applicablePermissions()).isEqualTo(roleResource.applicablePermissions());
    }

    @Test
    public void testWrappingNullResourceThrows()
    {
        assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> GrantResource.fromResource(null));
    }
}