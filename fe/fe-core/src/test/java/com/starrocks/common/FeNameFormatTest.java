// Copyright 2021-present StarRocks, Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// This file is based on code available under the Apache license here:
//   https://github.com/apache/incubator-doris/blob/master/fe/fe-core/src/test/java/org/apache/doris/common/FeNameFormatTest.java

// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.starrocks.common;

import com.starrocks.server.RunMode;
import com.starrocks.sql.analyzer.FeNameFormat;
import com.starrocks.sql.analyzer.SemanticException;
import mockit.Mock;
import mockit.MockUp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static com.starrocks.sql.analyzer.FeNameFormat.SPECIAL_CHARACTERS_IN_DB_NAME;

public class FeNameFormatTest {

    @Test
    public void testCheckColumnName() {

        ExceptionChecker.expectThrowsNoException(() -> FeNameFormat.checkColumnName("_id"));
        ExceptionChecker.expectThrowsNoException(() -> FeNameFormat.checkColumnName("01"));
        ExceptionChecker.expectThrowsNoException(() -> FeNameFormat.checkColumnName("Space Test"));
        ExceptionChecker.expectThrowsNoException(() -> FeNameFormat.checkColumnName("@timestamp"));

        String rndStr = "0123456789qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM";
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 64; i++) {
            sb.append(rndStr.charAt(r.nextInt(rndStr.length())));
        }
        // length 64
        ExceptionChecker.expectThrowsNoException(() -> FeNameFormat.checkColumnName(sb.toString()));
        ExceptionChecker.expectThrows(SemanticException.class, () -> FeNameFormat.checkColumnName("Column \0 Name"));
        ExceptionChecker.expectThrows(SemanticException.class, () -> FeNameFormat.checkColumnName("\0"));
        // length 0
        ExceptionChecker.expectThrows(SemanticException.class, () -> FeNameFormat.checkColumnName(""));
    }

    @Test
    public void testCheckDbName() {
        String prefix = "a";
        String dbName = prefix;
        for (char c : SPECIAL_CHARACTERS_IN_DB_NAME) {
            dbName += c;
        }

        while (dbName.length() < 256) {
            dbName += "a";
        }
        String finalDbName = dbName;
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkDbName(finalDbName));

        dbName += "a";
        String finalDbName1 = dbName;
        Assertions.assertThrows(SemanticException.class, () -> FeNameFormat.checkDbName(finalDbName1));

        prefix = "_a";
        dbName = prefix;
        for (char c : SPECIAL_CHARACTERS_IN_DB_NAME) {
            dbName += c;
        }

        while (dbName.length() < 256) {
            dbName += "a";
        }
        String finalDbName2 = dbName;
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkDbName(finalDbName2));


        Assertions.assertThrows(SemanticException.class, () -> FeNameFormat.checkDbName("!abc"));
        Assertions.assertThrows(SemanticException.class, () -> FeNameFormat.checkDbName("ab.c"));
        Assertions.assertThrows(SemanticException.class, () -> FeNameFormat.checkDbName("ab c"));
        Assertions.assertThrows(SemanticException.class, () -> FeNameFormat.checkDbName("ab\0c"));
    }

    @Test
    public void testCheckTableName() {
        // length boundary: 1024 chars is accepted, 1025 is rejected
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkTableName("a".repeat(1024)));
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkTableName("my_table"));
        Assertions.assertThrows(SemanticException.class, () -> FeNameFormat.checkTableName("a".repeat(1025)));
        Assertions.assertThrows(SemanticException.class, () -> FeNameFormat.checkTableName(""));
        Assertions.assertThrows(SemanticException.class, () -> FeNameFormat.checkTableName("ab\0c"));
    }

    @Test
    public void testCheckNameTooLongMessage() {
        // over-length names report ERR_TOO_LONG_IDENT ("is too long"), not the generic wrong-name error
        ExceptionChecker.expectThrowsWithMsg(SemanticException.class, "is too long",
                () -> FeNameFormat.checkTableName("a".repeat(1025)));
        ExceptionChecker.expectThrowsWithMsg(SemanticException.class, "is too long",
                () -> FeNameFormat.checkDbName("a".repeat(257)));
        ExceptionChecker.expectThrowsWithMsg(SemanticException.class, "is too long",
                () -> FeNameFormat.checkColumnName("a".repeat(1025)));
    }

    @Test
    public void testCheckNamespace() {
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkNamespace("abc"));
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkNamespace("ns1.ns2"));
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkNamespace("ns1.ns2.ns3"));
        // leading special character is rejected
        Assertions.assertThrows(SemanticException.class, () -> FeNameFormat.checkNamespace("!ns"));
        // over-length (> 256) reports ERR_TOO_LONG_IDENT
        ExceptionChecker.expectThrowsWithMsg(SemanticException.class, "is too long",
                () -> FeNameFormat.checkNamespace("a".repeat(257)));
    }

    @Test
    public void testCheckPartitionName() {
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkPartitionName("p1"));
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkPartitionName("_p1"));
        // invalid character / empty go through the generic wrong-name error
        Assertions.assertThrows(SemanticException.class, () -> FeNameFormat.checkPartitionName("!p"));
        Assertions.assertThrows(SemanticException.class, () -> FeNameFormat.checkPartitionName(""));
        // over-length (> 64) reports ERR_TOO_LONG_IDENT
        ExceptionChecker.expectThrowsWithMsg(SemanticException.class, "is too long",
                () -> FeNameFormat.checkPartitionName("a".repeat(65)));
        // forbidden placeholder prefix is rejected even when the format is otherwise valid
        Assertions.assertThrows(SemanticException.class,
                () -> FeNameFormat.checkPartitionName(FeNameFormat.FORBIDDEN_PARTITION_NAME + "x"));
    }

    @Test
    public void testCheckCommonName() {
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkResourceName("res1"));
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkCatalogName("cat1"));
        // invalid character / empty go through the generic wrong-name-format error
        Assertions.assertThrows(SemanticException.class, () -> FeNameFormat.checkResourceName("!res"));
        Assertions.assertThrows(SemanticException.class, () -> FeNameFormat.checkWarehouseName(""));
        // over-length (> 64) reports ERR_TOO_LONG_IDENT
        ExceptionChecker.expectThrowsWithMsg(SemanticException.class, "is too long",
                () -> FeNameFormat.checkStorageVolumeName("a".repeat(65)));
    }

    @Test
    public void testCheckColNameInSharedNothing() {
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkColumnName("abc.abc"));
        Assertions.assertThrows(SemanticException.class, () -> FeNameFormat.checkColumnName("!abc"));
        Assertions.assertThrows(SemanticException.class, () -> FeNameFormat.checkColumnName("!abc"));
        Assertions.assertThrows(SemanticException.class, () -> FeNameFormat.checkColumnName("abc<>"));
        Assertions.assertThrows(SemanticException.class, () -> FeNameFormat.checkColumnName("$abc!abc"));

    }

    @Test
    public void testCheckColNameInSharedData() {
        new MockUp<RunMode>() {
            @Mock
            public RunMode getCurrentRunMode() {
                return RunMode.SHARED_DATA;
            }
        };

        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkColumnName("abc.abc"));
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkColumnName("!abc"));
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkColumnName("abc<>"));
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkColumnName("$abc!abc"));
    }

    @Test
    public void testCheckUserName() {
        // length boundary: 128 chars accepted, 129 reports ERR_TOO_LONG_IDENT
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkUserName("a".repeat(128)));
        ExceptionChecker.expectThrowsWithMsg(SemanticException.class, "is too long",
                () -> FeNameFormat.checkUserName("a".repeat(129)));
        // kerberos principal/host whose total length exceeds 64 is accepted -- this used to be wrongly rejected by a
        // hard-coded length() > 64 guard even though the format allowed it (see #2603).
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkUserName("service/" + "a".repeat(60) + ".example.com"));
    }

    // Covers every branch of MYSQL_USER_NAME_REGEX = ^\w+/?[.\w-]*$ that must be accepted.
    @ParameterizedTest
    @ValueSource(strings = {
            "a",                                     // minimal \w+ (single word char)
            "root",                                  // \w+ letters only, no slash, no host part
            "_user1",                                // leading underscore + digits (all \w)
            "User_123",                              // mixed case + underscore + digit in the principal
            "123user",                               // \w allows a leading digit (user names differ from db/role here)
            "user.name",                             // no slash: '.' is consumed by the trailing [.\w-]* group
            "user-name",                             // no slash: '-' is consumed by the trailing [.\w-]* group
            "user/",                                 // optional '/' present, host part empty ([.\w-]* matches zero)
            "stephen/host01.example.com",            // typical kerberos principal/host with dots
            "HTTP/web-server.example.com",           // uppercase service principal + '-' in the host
            "svc/my-host.sub_domain.example-1.com"   // host part mixing '.', '-', '_' and word chars
    })
    public void testCheckUserNameValidFormats(String userName) {
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkUserName(userName));
    }

    // Covers the inputs MYSQL_USER_NAME_REGEX must reject; all surface the generic "invalid user name" format error.
    @ParameterizedTest
    @ValueSource(strings = {
            "",            // empty (isNullOrEmpty branch)
            "/host",       // leading '/': \w+ requires at least one leading word char
            ".host",       // leading '.'
            "-host",       // leading '-'
            "a/b/c",       // more than one '/' separator (only a single optional '/' is allowed)
            "aaa~bbb",     // '~' is outside \w and [.\w-]
            "user@host",   // '@' not allowed
            "user host",   // space not allowed in the principal
            "user/ho st",  // space not allowed in the host part
            "user/host@x", // '@' not allowed in the host part
            "user#1",      // '#' not allowed
            "user!"        // '!' not allowed
    })
    public void testCheckUserNameInvalidFormats(String userName) {
        ExceptionChecker.expectThrowsWithMsg(SemanticException.class, "invalid user name",
                () -> FeNameFormat.checkUserName(userName));
    }

    @Test
    public void testCheckRoleName() {
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkRoleName("role1", true, "invalid role"));
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkRoleName("_role", true, "invalid role"));
        // length boundary: 64 chars accepted, 65 reports ERR_TOO_LONG_IDENT
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkRoleName("a".repeat(64), true, "invalid role"));
        ExceptionChecker.expectThrowsWithMsg(SemanticException.class, "is too long",
                () -> FeNameFormat.checkRoleName("a".repeat(65), true, "invalid role"));
        // empty / character violations still report the generic "invalid role format" error
        ExceptionChecker.expectThrowsWithMsg(SemanticException.class, "invalid role format",
                () -> FeNameFormat.checkRoleName("___", true, "invalid role"));
        ExceptionChecker.expectThrowsWithMsg(SemanticException.class, "invalid role format",
                () -> FeNameFormat.checkRoleName("", true, "invalid role"));
    }

    @Test
    public void testCheckLabel() {
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkLabel("label_1"));
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkLabel("my-label"));
        // length boundary: 128 chars accepted, 129 reports ERR_TOO_LONG_IDENT
        Assertions.assertDoesNotThrow(() -> FeNameFormat.checkLabel("a".repeat(128)));
        ExceptionChecker.expectThrowsWithMsg(SemanticException.class, "is too long",
                () -> FeNameFormat.checkLabel("a".repeat(129)));
        // empty / character violations still report the generic "Label format error"
        ExceptionChecker.expectThrowsWithMsg(SemanticException.class, "Label format error",
                () -> FeNameFormat.checkLabel("bad label"));
        ExceptionChecker.expectThrowsWithMsg(SemanticException.class, "Label format error",
                () -> FeNameFormat.checkLabel(""));
    }

}
