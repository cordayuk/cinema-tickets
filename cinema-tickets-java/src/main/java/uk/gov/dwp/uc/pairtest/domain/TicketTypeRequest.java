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
        ADULT(25), CHILD(15) , INFANT(0);

        int cost;

        Type(int cost) {
            this.cost = cost;
        }

        public int getCost() {
            return cost;
        }
    }



}
