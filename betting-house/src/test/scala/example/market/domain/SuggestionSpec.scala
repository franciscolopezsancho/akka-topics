//     Scenario 1
// t0 - Fixture Real Madrid vs Manchester United.
// t1 - Market1 created.
// ...
//  - Markets close
//  - Ann bets Market3 and gets rejected

//     Scenario 2
// t0 - Fixture Real Madrid vs Manchester United.
// t1 - Market1 created.
// ...
//  - Ann bets before the event starts and the bets gets scheduled

//     Scenario 3
// t0 - Fixture Real Madrid vs Manchester United.
// t1 - Market1 created.
// ...
//  - Ann bets before the event starts and the bets gets scheduled
//  - The event starts
//  - The bet checks the market and gets validated

//     Scenario 4
// t0 - Fixture Real Madrid vs Manchester United.
// t1 - Market1 created.
// ...
//  - Ann bets after the event starts
//  - The bet checks the market and gets validated
//  - It doesn't pass the validation, the market has moved
//  - It answers to the customer the new odd
//  - it holds the bet, and waits for response.
//  - the answer comes back and gets accepted

//     Scenario 5
// t0 - Fixture Real Madrid vs Manchester United.
// t1 - Market1 created.
// ...
//  - Ann bets after the event starts
//  - The bet checks the market and gets validated
//  - It doesn't pass the validation, the market has moved
//  - It answers to the customer the new odd
//  - it holds the bet, and waits for response.
//  - the answer comes back and gets rejected (the user doesn't want it)

//     Scenario 6
// t0 - Fixture Real Madrid vs Manchester United.
// t1 - Market1 created.
// ...
//  - Ann bets after the event starts
//  - The bet checks the market and gets validated
//  - It doesn't pass the validation, the market has moved
//  - It answers to the customer the new odd
//  - it holds the bet, and waits for response.
//  - the answer does NOT come back
//  - the bet gets Rejected.
