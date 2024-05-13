export type JwtDetails = {
    readonly username: string,
    readonly issuedAt: number,
    readonly expiresAt: number,
    readonly expiresLeft: number,
    readonly authorities: string[];
}
