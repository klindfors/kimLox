package com.craftinginterpreters.lox;

enum TokenType {


    // Water flow tokens
    RW, T, A, // River, Dam, rW for water array, T for terrain, A for area size
    THRESHOLD, CAP, OPENING,
    RSTAR, DSTAR,
    PSTAR,        // *P
    PRSTAR,    // *Prand


    // Single-character tokens.

    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    LEFT_BRACKET, RIGHT_BRACKET, //added for  rW arrays [...]
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



    AND, CLASS, ELSE, FALSE, IF, FOR, FUN, NIL, OR,
    PRINT, RETURN, TRUE, VAR, WHILE,

    EOF
}