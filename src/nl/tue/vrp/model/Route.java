package nl.tue.vrp.model;

import nl.tue.vrp.model.nodes.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class Route {

    private final Vehicle vehicle;
    private final Visit firstVisit;
    private final List<Visit> visits;

    public Route(Node origin, Vehicle vehicle, List<Node> nodes, BiFunction<Visit, List<Node>, Node> nodeSearchStrategy) {
        this.vehicle = vehicle;
        this.firstVisit = new Visit(vehicle, origin);
        this.firstVisit.addNextVisit(origin);
        Visit currentVisit = this.firstVisit;

        List<Node> remainingNodes = new ArrayList<>(nodes);
        List<Node> skippedNodes = new ArrayList<>();

        boolean skip;
        while (!remainingNodes.isEmpty()) {
            Node nextNode = nodeSearchStrategy.apply(currentVisit, remainingNodes);
            if (visitFeasible(currentVisit, nextNode)) {
                currentVisit = currentVisit.addNextVisit(nextNode);
                remainingNodes.remove(nextNode);
                skip = false;
            } else {
                skip = true;
                remainingNodes.remove(nextNode);
                skippedNodes.add(nextNode);
            }
            if (!skip) {
                remainingNodes.addAll(skippedNodes);
                skippedNodes.clear();
            }
        }

        List<Visit> tVisits = new ArrayList<>();
        Visit tVisit = this.firstVisit;
        tVisits.add(tVisit);
        while (tVisit.getNext().isPresent()) {
            tVisit = tVisit.getNext().get();
            tVisits.add(tVisit);
        }
        this.visits = tVisits.parallelStream().collect(Collectors.toUnmodifiableList());
    }

    private boolean visitFeasible(Visit currentVisit, Node nextNode) {
        Visit v = currentVisit;
        if (nextNode.isDelivery()) {
            boolean safe = v.getLoad() + nextNode.getDemand() <= vehicle.getCapacity();
            while (safe && v.getPrev().isPresent()) {
                v = v.getPrev().get();
                safe = v.getLoad() + nextNode.getDemand() <= vehicle.getCapacity();
            }
            return safe;
        } else if (nextNode.isPickUp()) {
            boolean safe = true;
            while (safe && v.getNext().isPresent()) {
                v = v.getNext().get();
                safe = v.getLoad() - nextNode.getDemand() <= vehicle.getCapacity();
            }
            return safe;
        } else {
            return true;
        }
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public List<Visit> getVisits() {
        return visits;
    }

    @Override
    public String toString() {
        return String.format("Route[%s]", getVisits().stream()
                .map(Visit::toString)
                .collect(Collectors.joining(",\n")));
    }
}
