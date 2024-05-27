import {Big} from "big.js";

export type GetPurchasesResponse = {
    purchaseId: number,
    purchaseTimestamp: number,
    productName: string,
    productPrice: Big
}
