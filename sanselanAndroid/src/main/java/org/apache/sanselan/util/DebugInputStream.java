/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.sanselan.util;

import java.io.IOException;
import java.io.InputStream;

@SuppressWarnings("ALL")
public class DebugInputStream extends InputStream
{
	private final InputStream is;

	public DebugInputStream(InputStream is)
	{
		this.is = is;
	}

	private long bytes_read = 0;

	public int read() throws IOException
	{
		int result = is.read();
		bytes_read++;
		return result;
	}

	public long skip(long n) throws IOException
	{
		long result = is.skip(n);
		bytes_read += n;
		return result;
	}

	public int available() throws IOException
	{
		return is.available();
	}

	public void close() throws IOException
	{
		is.close();
	}

	public long getBytesRead()
	{
		return bytes_read;
	}
}
