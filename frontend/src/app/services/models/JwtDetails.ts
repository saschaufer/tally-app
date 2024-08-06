export type JwtDetails = {
    readonly email: string,
    readonly issuedAt: number,
    readonly expiresAt: number,
    readonly expiresLeft: number,
    readonly authorities: string[];
}
