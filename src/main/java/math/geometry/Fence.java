package math.geometry;

import java.util.List;

public class Fence {
	// 포인트 목록, 순서대로 연결된 것으로 처리된다
	private List<Point2D> points;
	// 닫힘 여부, 닫혀 있을 경우 마지막 포인트와 첫 포인트를 연결한 벽과의 충돌 처리를 수행한다
	private boolean closed;
	// 맞닿은 것으로 판정되는 거리, 시작지점으로 부터 해당 거리 이내의 벽과는 충돌 처리를 하지 않는다
	private double minSpace;
	
	public Fence(List<Point2D> points, boolean closed, double minSpace) {
		this.points = points;
		this.closed = closed;
		this.minSpace = minSpace;
	}
	
	public boolean collide(Move2D before, Move2D after) {
		Point2D start = before.getStart();
		Point2D direction = before.getDirection();
		double min = Double.MAX_VALUE;
		Point2D minP1 = null;
		Point2D minP2 = null;
		double nearx = Double.NaN;
		double neary = Double.NaN;
		for (int i = 0, n = points.size(), c = closed ? n : n - 1; i < c; i++) {
			Point2D p1 = points.get(i);
			Point2D p2 = points.get((i + 1) % n);
			double r;
			try {
				// 시작지점에서 진행방향으로 벽과 만나는 지점이 있는지 찾는다.
				r = Geometry.encounterPositionInSegment(p1.getX(), p1.getY(), p2.getX(), p2.getY(), start.getX(), start.getY(), direction.getX(), direction.getY());
			} catch (GeometryException e) {
				continue;
			}
			// 벽과 만나는 지점 계산
			double x = p1.getX() * (1 - r) + p2.getX() * r;
			double y = p1.getY() * (1 - r) + p2.getY() * r;
			// 시작지점과 만나는지점 사이의 거리 계산
			double ds = Geometry.hypotSquare(start.getX() - x, start.getY() - y);
			// 최소 거리일 경우 정보 갱신
			if (min > ds && ds >= minSpace) {
				min = ds;
				minP1 = p1;
				minP2 = p2;
				nearx = x;
				neary = y;
			}
		}
		if (minP1 == null) {
			return false;
		}
		after.getStart().set(nearx, neary);
		// 반사벡터를 구하기 위해 벽에 대한 방향벡터의 정사영을 구한다.
		// (공식)반사벡터 = -방향벡터 + 2 * 정사영
		double px = minP2.getX() - minP1.getX();
		double py = minP2.getY() - minP1.getY();
		double shadowSize = (direction.getX() * px + direction.getY() * py) / Geometry.hypotSquare(px, py);
		double sx = shadowSize * px;
		double sy = shadowSize * py;
		after.getDirection().set(-before.getDirection().getX() + 2 * sx, -before.getDirection().getY() + 2 * sy);
		return true;
	}
}
