package uk.gov.dwp.uc.pairtest;

import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */

    private static final int MAX_TICKETS = 25;

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl(TicketPaymentService ticketPaymentService, SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {

        validateRequest(accountId, ticketTypeRequests);

        Map<Type, Integer> ticketCounts = tallyTicketCounts(ticketTypeRequests);

        validateBusinessRules(ticketCounts);

        int amountToPay = ticketCounts.entrySet().stream().mapToInt(entry -> entry.getValue() * entry.getKey().getCost()).sum();
        int totalSeats = ticketCounts.entrySet().stream().filter(e -> e.getKey().isSeatRequired()).mapToInt(Entry::getValue).sum();
        ticketPaymentService.makePayment(accountId, amountToPay);
        seatReservationService.reserveSeat(accountId, totalSeats);
    }

    private void validateBusinessRules(Map<Type, Integer> ticketCounts) {
        int ticketSum = ticketCounts.values().stream().mapToInt(Integer::valueOf).sum();

        if(ticketSum == 0) {
            throw new InvalidPurchaseException("You must request at least 1 ticket");
        }

        if(ticketSum > MAX_TICKETS) {
            throw new InvalidPurchaseException(String.format("You cannot request more than %d tickets", MAX_TICKETS));
        }

        if(!ticketCounts.containsKey(ADULT) || ticketCounts.get(ADULT) == 0){
            throw new InvalidPurchaseException("At least 1 ADULT ticket is required");
        }

        if(ticketCounts.containsKey(INFANT) && ticketCounts.get(INFANT) > ticketCounts.get(ADULT)) {
            throw new InvalidPurchaseException("You cannot have more INFANTS than ADULTS");
        }
    }

    private Map<Type, Integer> tallyTicketCounts(TicketTypeRequest[] ticketTypeRequests) {
        Map<Type, Integer> ticketTally = new EnumMap<>(Type.class);

        for(TicketTypeRequest ticketTypeRequest : ticketTypeRequests) {
            if(ticketTypeRequest == null){
                throw new InvalidPurchaseException("Ticket request must not be null");
            }
            Type ticketType = ticketTypeRequest.type();
            ticketTally.put(ticketType,ticketTally.getOrDefault(ticketType, 0) + ticketTypeRequest.noOfTickets());
        }

        return ticketTally;
    }

    private void validateRequest(Long accountId, TicketTypeRequest[] ticketTypeRequests) throws InvalidPurchaseException {

        if(accountId == null || accountId < 1L ){
            throw new InvalidPurchaseException("You must provide a valid account Id");
        }

        if(ticketTypeRequests == null || ticketTypeRequests.length == 0){
            throw new InvalidPurchaseException("You must request at least 1 ticket");
        }
    }

}
