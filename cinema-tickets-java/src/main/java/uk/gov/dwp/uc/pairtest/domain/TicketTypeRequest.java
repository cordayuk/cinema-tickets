package uk.gov.dwp.uc.pairtest.domain;

/**
 * Immutable Object
 *
 * Represents a request to purchase x number of tickets of a certain type
 */

public record TicketTypeRequest(Type type, int noOfTickets) {
    public TicketTypeRequest {
        if(type == null){
            throw new IllegalArgumentException("Type cannot be null");
        }
        if(noOfTickets < 0){
            throw new IllegalArgumentException("noOfTickets cannot be less than 0");
        }
    }

    public enum Type {
        ADULT(25, true),
        CHILD(15, true),
        INFANT(0, false);

        int cost;
        boolean seatRequired;

        Type(int cost, boolean seatRequired) {
            this.cost = cost;
            this.seatRequired = seatRequired;
        }

        public int getCost() {
            return cost;
        }

        public boolean isSeatRequired() {
            return seatRequired;
        }
    }



}
