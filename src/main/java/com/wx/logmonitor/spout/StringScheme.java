/*
 * (C) 2007-2012 Alibaba Group Holding Limited.
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
 * Authors:
 *   wuhua <wq163@163.com> , boyan <killme2008@gmail.com>
 */
package com.wx.logmonitor.spout;

import org.apache.storm.spout.Scheme;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

import java.nio.ByteBuffer;
import java.util.List;
/*
  Scheme  继承了Serializable类，所以这是一个把集合对象一起序列化成网络传输对象的工具
 */
public class StringScheme implements Scheme {
    @Override
    public List<Object> deserialize(ByteBuffer byteBuffer) {
        try {
            //byteBuffer转化为byte[] 再转化为String
            return new Values(new String(byteBuffer.array()));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    public Fields getOutputFields() {
        return new Fields("line");
    }
}