package com.craftinginterpreters.lox;

enum TokenType {
    // Single-character tokens.
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    LEFT_BRACKET, RIGHT_BRACKET, // Added for arrays [...]
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,
    SUPER, THIS,

    // One or two character tokens.
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,


    ROOT,        // ^ for root rivers
    AMPERSAND,    // & for river joins
    AT,           // @ for flow notation

    // Literals.
    IDENTIFIER, STRING, NUMBER,

    // Water flow keywords
    RIVER, RW, T, A, // River, Dam, rW for water array, T for terrain, A for area size
    DAM, THRESHOLD, CAP, OPENING,

    AND, CLASS, ELSE, FALSE, IF, FOR, FUN, NIL, OR,
    PRINT, RETURN, TRUE, VAR, WHILE,

    EOF
}