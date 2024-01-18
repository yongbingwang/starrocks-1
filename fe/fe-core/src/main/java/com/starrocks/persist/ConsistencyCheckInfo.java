// This file is made available under Elastic License 2.0.
// This file is based on code available under the Apache license here:
//   https://github.com/apache/incubator-doris/blob/master/fe/fe-core/src/main/java/org/apache/doris/persist/ConsistencyCheckInfo.java

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

package com.starrocks.persist;

import com.starrocks.common.io.Writable;
import com.starrocks.persist.gson.GsonUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ConsistencyCheckInfo implements Writable {

    private long dbId;
    private long tableId;
    private long partitionId;
    private long indexId;
    private long tabletId;

    private long lastCheckTime;

    private long checkedVersion;

    private boolean isConsistent;

    public ConsistencyCheckInfo() {
        // for persist
    }

    public ConsistencyCheckInfo(long dbId, long tableId, long partitionId, long indexId, long tabletId,
                                long lastCheckTime, long checkedVersion,
                                boolean isConsistent) {
        this.dbId = dbId;
        this.tableId = tableId;
        this.partitionId = partitionId;
        this.indexId = indexId;
        this.tabletId = tabletId;

        this.lastCheckTime = lastCheckTime;
        this.checkedVersion = checkedVersion;

        this.isConsistent = isConsistent;
    }

    public long getDbId() {
        return dbId;
    }

    public long getTableId() {
        return tableId;
    }

    public long getPartitionId() {
        return partitionId;
    }

    public long getIndexId() {
        return indexId;
    }

    public long getTabletId() {
        return tabletId;
    }

    public long getLastCheckTime() {
        return lastCheckTime;
    }

    public long getCheckedVersion() {
        return checkedVersion;
    }

    public boolean isConsistent() {
        return isConsistent;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeLong(dbId);
        out.writeLong(tableId);
        out.writeLong(partitionId);
        out.writeLong(indexId);
        out.writeLong(tabletId);

        out.writeLong(lastCheckTime);
        out.writeLong(checkedVersion);
        out.writeLong(0); // write a version_hash for compatibility

        out.writeBoolean(isConsistent);
    }

    public void readFields(DataInput in) throws IOException {
        dbId = in.readLong();
        tableId = in.readLong();
        partitionId = in.readLong();
        indexId = in.readLong();
        tabletId = in.readLong();

        lastCheckTime = in.readLong();
        checkedVersion = in.readLong();
        in.readLong(); // read a version_hash for compatibility

        isConsistent = in.readBoolean();
    }

    public static ConsistencyCheckInfo read(DataInput in) throws IOException {
        ConsistencyCheckInfo info = new ConsistencyCheckInfo();
        info.readFields(in);
        return info;
    }

    @Override
    public String toString() {
        return GsonUtils.GSON.toJson(this);
    }
}
