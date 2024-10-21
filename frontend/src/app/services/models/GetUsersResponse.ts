import {Big} from "big.js";

export type GetUsersResponse = {
    email: string,
    registrationOn: number,
    registrationComplete: boolean,
    roles: string[],
    accountBalance: Big
}