export type LoginResponse = {
    readonly jwt: string;
    readonly secure: boolean;
    readonly properties: Properties;
}

export type Properties = {
    readonly currency: string;
}
