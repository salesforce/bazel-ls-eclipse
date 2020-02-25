package com.salesforce.b2eclipse;

import static org.junit.Assert.assertEquals;

import com.salesforce.b2eclipse.BazelNature;
import org.eclipse.core.resources.IProject;
import org.junit.Test;

public class BazelNatureTest {
	
	@Test
    public void testGetProject() {
		IProject expected = null;
		BazelNature nature = new BazelNature();
		IProject actual = nature.getProject();
		assertEquals(actual, expected);
		System.out.println(nature);
	}

}
