export type Jwt = {
    readonly iss: string;
    readonly sub: string;
    readonly aud: string;
    readonly exp: number;
    readonly iat: number;
    readonly authorities: string[];
}
