package math.geometry;

public class Geometry {
	static public double perpendicularPositionInLine(double sx1, double sy1, double sx2, double sy2, double x, double y) {
		double dlx1 = x - sx1;
		double dly1 = y - sy1;
		double dlx2 = x - sx2;
		double dly2 = y - sy2;
		double blx = sx2 - sx1;
		double bly = sy2 - sy1;
		double l1 = dlx1 * blx + dly1 * bly;
		double l2 = -(dlx2 * blx + dly2 * bly);
		return l1 / (l1 + l2);
	}
	
	static public double perpendicularPositionInSegment(double sx1, double sy1, double sx2, double sy2, double x, double y) {
		double position = perpendicularPositionInLine(sx1, sy1, sx2, sy2, x, y);
		return Math.min(Math.max(position, 0), 1);
	}
	
	static public double encounterPositionInLine(double sx1, double sy1, double sx2, double sy2, double x, double y, double dx, double dy) throws GeometryException {
		double dlx1 = x - sx1;
		double dly1 = y - sy1;
		double dlx2 = x - sx2;
		double dly2 = y - sy2;
		double blx = sx2 - sx1;
		double bly = sy2 - sy1;
		double l1 = dlx1 * blx + dly1 * bly;
		double l2 = -(dlx2 * blx + dly2 * bly);
		
		double fx = (sx1 * l2 + sx2 * l1) / (l1 + l2);
		double fy = (sy1 * l2 + sy2 * l1) / (l1 + l2);
		double px = fx - x;
		double py = fy - y;
		double h2 = hypotSquare(x - fx, y - fy);
		double ipPD = dx * px + dy * py;
		if (ipPD <= 0) {
			// 수선의 방향과 직선의 방향이 90도 이상으로 벌어져 서로 만날 수 없다.
			throw new GeometryException("("+sx1+", "+sy1+") 에서 ("+sx2+", "+sy2+") 로의 직선과 ("+x+", "+y+")에서 ("+dx+", "+dy+")방향의 직선은 서로 만나지 못한다.");
		}
		double mul = h2 / ipPD;
		double l3 = -(dx * blx + dy * bly) * mul;	// dx, dy의 반대 방향 벡터가 필요
		return (l1 - l3) / (l1 + l2);
	}
	
	static public double encounterPositionInSegment(double sx1, double sy1, double sx2, double sy2, double x, double y, double dx, double dy) throws GeometryException {
		double position = encounterPositionInLine(sx1, sy1, sx2, sy2, x, y, dx, dy);
		if (position < 0 || position > 1) {
			// 선분 밖에서 만난다.
			throw new GeometryException("("+sx1+", "+sy1+") 에서 ("+sx2+", "+sy2+") 로의 선분과 ("+x+", "+y+")에서 ("+dx+", "+dy+")방향의 직선은 서로 만나지 못한다.");
		}
		return position;
	}
	
	static public double distanceSquareFromPointToSegmentPosition(double sx1, double sy1, double sx2, double sy2, double x, double y, double r) {
		double lx = sx1 * (1 - r) + sx2 * r;
		double ly = sx1 * (1 - r) + sx2 * r;
		return hypotSquare(lx - x, ly - y);
	}
	
	static public double hypotSquare(double x, double y) {
		return x * x + y * y;
	}
}
