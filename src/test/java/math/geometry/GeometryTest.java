package math.geometry;

import org.junit.Assert;
import org.junit.Test;

public class GeometryTest {
	@Test
	public void test() throws GeometryException {
		Assert.assertEquals(0.5, Geometry.perpendicularPositionInSegment(0, 0, 1, 0, 0.5, 1), 0.0001);
		Assert.assertEquals(0.25, Geometry.perpendicularPositionInSegment(0, 1, 1, 0, 0.5, 1), 0.0001);
		Assert.assertEquals(0.0, Geometry.perpendicularPositionInSegment(0, 0, 1, 0, -.5, 1), 0.0001);
		Assert.assertEquals(-0.5, Geometry.perpendicularPositionInLine(0, 0, 1, 0, -.5, 1), 0.0001);
		Assert.assertEquals(1.0, Geometry.perpendicularPositionInSegment(0, 0, 1, 0, 1.5, 1), 0.0001);
		Assert.assertEquals(1.5, Geometry.perpendicularPositionInLine(0, 0, 1, 0, 1.5, 1), 0.0001);

		Assert.assertEquals(0.25, Geometry.encounterPositionInLine(0, 0, 1, 0, 0.5, 1, -0.25, -1), 0.0001);
		Assert.assertEquals(0.75, Geometry.encounterPositionInLine(0, 0, 1, 0, 0.5, 1, 0.25, -1), 0.0001);
		Assert.assertEquals(1.5, Geometry.encounterPositionInLine(0, 1, 1, 0, 0.5, 1, 1, -1.5), 0.0001);
	}
	
	@Test(expected = GeometryException.class) 
	public void testGeometryException() throws GeometryException {
		Geometry.encounterPositionInSegment(0, 1, 1, 0, 0.5, 1, 1, -1.5);
	}
}
