package com.mike;

import com.carrotsearch.randomizedtesting.ThreadFilter;

public class TestRunnerThreadsFilter implements ThreadFilter {

	@Override
	public boolean reject(Thread thread) {
		// randomizedtesting shouldn't bother checking for zombie threads for these:
		String threadName = thread.getName();

		if (threadName.startsWith("elasticsearch")) {
			return true;
		}
		
		return false;
	}

}
