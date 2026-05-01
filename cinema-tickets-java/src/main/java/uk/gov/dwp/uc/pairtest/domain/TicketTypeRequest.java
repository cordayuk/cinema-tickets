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
        if(noOfTickets < 1){
            throw new IllegalArgumentException("noOfTickets cannot be less than 1");
        }
    }

    public enum Type {
        ADULT, CHILD , INFANT
    }

}
