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

    /**
     * Purchases tickets for the given account, charges the payment service and reserves the number of
     * required seats.
     *
     * The request is validated, ticket quantities are tallied by {@link Type} and the business rules
     * are enforced. Once validated the total cost is calculated and paid via {@link TicketPaymentService}
     * and then the seats are reserved via {@link SeatReservationService}.
     *
     * @param accountId the ID of the account requesting tickets. Must non-null and >= 1.
     * @param ticketTypeRequests one or more {@link TicketTypeRequest} objects that represent the
     *                           type and quantity of tickets requested. Must not be null or empty,
     *                           and must not contain null elements.
     * @throws InvalidPurchaseException if accountID is null or <1, if ticketTypeRequests
     *                                  is null/empty or contains a null element or the request
     *                                  does not meet the business rules.
     */
    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {

        validateRequest(accountId, ticketTypeRequests);

        Map<Type, Integer> ticketCounts = tallyTicketCounts(ticketTypeRequests);

        validateBusinessRules(ticketCounts);

        int amountToPay = calculateTotalCost(ticketCounts);
        int totalSeats = calculateTotalSeatsRequired(ticketCounts);

        ticketPaymentService.makePayment(accountId, amountToPay);
        seatReservationService.reserveSeat(accountId, totalSeats);
    }

    /**
     * Calculates the total cost of all tickets across all ticket types.
     *
     * @param ticketCounts a Map of {@link Type} to the total quantity of tickets request for that type.
     *
     * @return the total cost of all ticket types in ticketCounts.
     */
    private int calculateTotalCost(Map<Type, Integer> ticketCounts) {
        return ticketCounts.entrySet().stream()
            .mapToInt(entry -> entry.getValue() * entry.getKey().getCost())
            .sum();
    }

    /**
     * Calculates the total number of seats required across all ticket types.
     * Ticket {@link Type) that do not require seats are excluded from the count.
     *
     * @param ticketCounts a Map of {@link Type} to the total quantity of tickets request for that type.
     * @return the total number of seats required.
     */
    private int calculateTotalSeatsRequired(Map<Type, Integer> ticketCounts) {
        return ticketCounts.entrySet().stream()
            .filter(entry -> entry.getKey().isSeatRequired())
            .mapToInt(Entry::getValue)
            .sum();
    }

    /**
     * Validates the ticket counts against the business rules.
     * Throws {@link InvalidPurchaseException} if validation fails.
     *
     * @param ticketCounts - the ticket count of each type of ticket.
     */
    private void validateBusinessRules(Map<Type, Integer> ticketCounts) {
        int totalTicketsRequested = ticketCounts.values().stream()
            .mapToInt(Integer::valueOf)
            .sum();

        if(totalTicketsRequested == 0) {
            throw new InvalidPurchaseException("You must request at least 1 ticket");
        }

        if(totalTicketsRequested > MAX_TICKETS) {
            throw new InvalidPurchaseException(String.format("You cannot request more than %d tickets", MAX_TICKETS));
        }

        if(ticketCounts.containsKey(CHILD) && (ticketCounts.getOrDefault(ADULT, 0) == 0)){
            throw new InvalidPurchaseException("At least 1 ADULT ticket is required");
        }

        if(ticketCounts.containsKey(INFANT) && (ticketCounts.get(INFANT) > ticketCounts.getOrDefault(ADULT, 0))) {
            throw new InvalidPurchaseException("You cannot have more INFANTS than ADULTS");
        }
    }

    /**
     * Aggregates ticketTypeRequests by type, summing the total number of tickets requested of each type.
     * If this method encounters a null ticketTypeRequest in ticketTypeRequests it will
     * throw a {@link InvalidPurchaseException}
     *
     * @param ticketTypeRequests and array of {@link TicketTypeRequest} objects that specify the
     *                           type of ticket and quantity of tickets requested.
     *
     * @return a Map where each key is a {@link Type} and the value is the total number of tickets
     * requested for that type.
     */
    private Map<Type, Integer> tallyTicketCounts(TicketTypeRequest[] ticketTypeRequests) {
        Map<Type, Integer> ticketTally = new EnumMap<>(Type.class);

        for(TicketTypeRequest ticketTypeRequest : ticketTypeRequests) {
            if(ticketTypeRequest == null){
                throw new InvalidPurchaseException("Ticket request must not be null");
            }
            Type ticketType = ticketTypeRequest.type();
            ticketTally.put(ticketType, ticketTally.getOrDefault(ticketType, 0) + ticketTypeRequest.noOfTickets());
        }

        return ticketTally;
    }

    /**
     * Validates the incoming request.
     *
     * @param accountId the requested accountID
     * @param ticketTypeRequests the requested tickets by type.
     * @throws InvalidPurchaseException if accountId is null or <1 or ticketTypeRequests is null or empty.
     */
    private void validateRequest(Long accountId, TicketTypeRequest[] ticketTypeRequests) throws InvalidPurchaseException {

        if(accountId == null || accountId < 1L ){
            throw new InvalidPurchaseException("You must provide a valid account Id");
        }

        if(ticketTypeRequests == null || ticketTypeRequests.length == 0){
            throw new InvalidPurchaseException("You must request at least 1 ticket");
        }
    }
}
