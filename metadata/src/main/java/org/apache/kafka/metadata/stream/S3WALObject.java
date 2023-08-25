/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.kafka.metadata.stream;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.kafka.common.metadata.WALObjectRecord;
import org.apache.kafka.common.metadata.WALObjectRecord.StreamIndex;
import org.apache.kafka.server.common.ApiMessageAndVersion;

public class S3WALObject {

    private final long objectId;

    private final int brokerId;
    private final Map<Long/*streamId*/, S3ObjectStreamIndex> streamsIndex;

    private final S3ObjectType objectType = S3ObjectType.UNKNOWN;

    public S3WALObject(long objectId, int brokerId, final Map<Long, S3ObjectStreamIndex> streamsIndex) {
        this.objectId = objectId;
        this.brokerId = brokerId;
        this.streamsIndex = streamsIndex;
    }

    public boolean intersect(long streamId, long startOffset, long endOffset) {
        S3ObjectStreamIndex streamIndex = streamsIndex.get(streamId);
        if (streamIndex == null) {
            return false;
        }
        if (endOffset <= streamIndex.getStartOffset() || startOffset >= streamIndex.getEndOffset()) {
            return false;
        }
        return true;
    }

    public ApiMessageAndVersion toRecord() {
        return new ApiMessageAndVersion(new WALObjectRecord()
            .setObjectId(objectId)
            .setBrokerId(brokerId)
            .setStreamsIndex(
                streamsIndex.values().stream()
                    .map(S3ObjectStreamIndex::toRecordStreamIndex)
                    .collect(Collectors.toList())), (short) 0);
    }

    public static S3WALObject of(WALObjectRecord record) {
        S3WALObject s3WalObject = new S3WALObject(record.objectId(), record.brokerId(),
            record.streamsIndex().stream().collect(Collectors.toMap(
                StreamIndex::streamId, S3ObjectStreamIndex::of)));
        return s3WalObject;
    }

    public Integer getBrokerId() {
        return brokerId;
    }

    public Map<Long, S3ObjectStreamIndex> getStreamsIndex() {
        return streamsIndex;
    }

    public Long objectId() {
        return objectId;
    }

    public S3ObjectType objectType() {
        return objectType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        S3WALObject that = (S3WALObject) o;
        return objectId == that.objectId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectId);
    }

    @Override
    public String toString() {
        return "S3WALObject{" +
            "objectId=" + objectId +
            ", brokerId=" + brokerId +
            ", streamsIndex=" + streamsIndex +
            ", objectType=" + objectType +
            '}';
    }
}
