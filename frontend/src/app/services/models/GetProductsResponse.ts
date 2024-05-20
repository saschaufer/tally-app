import {Big} from "big.js";

export type GetProductsResponse = {
    readonly id: number,
    readonly name: string,
    readonly price: Big
}
