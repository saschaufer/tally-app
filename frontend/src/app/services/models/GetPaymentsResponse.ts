import {Big} from "big.js";

export type GetPaymentsResponse = {
    id: number,
    amount: Big,
    timestamp: number
}
