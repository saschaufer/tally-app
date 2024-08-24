import {Big} from "big.js";

export type GetAccountBalanceResponse = {
    amountPayments: Big,
    amountPurchases: Big,
    amountTotal: Big
}
