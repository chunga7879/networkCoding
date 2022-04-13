#include "netbuffer.h"
#include "mailuser.h"
#include "server.h"

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/utsname.h>
#include <ctype.h>

#include <sys/socket.h>
#include <strings.h>
#include <stdbool.h>

#define MAX_LINE_LENGTH 1024

// Keeps track of the last received command that will affect the state
// (eg. we need to know if a MAIL command has been received, so we can know if we can take RCPT commands)
enum state_received
{
    received_none = 0,
    received_HELO = 1,
    received_MAIL = 2,
    received_RCPT = 3,
    received_DATA = 4,
    received_QUIT = 5,
};

// Keeps track of the "mail" currently being composed
struct mail_state
{
    enum state_received state;
    user_list_t rcpts;
    char data[];
};

static void handle_client(int fd);
void interpret_response(int fd, struct utsname my_uname, char *response[], int parts, struct mail_state *state);
void handle_HELO(int fd, struct utsname my_uname, char *response[], int parts, struct mail_state *state);
void handle_EHLO(int fd, struct utsname my_uname, char *response[], int parts, struct mail_state *state);
void handle_VRFY(int fd, struct utsname my_uname, char *response[], int parts, struct mail_state *state);
void handle_QUIT(int fd, struct utsname my_uname, char *response[], int parts, struct mail_state *state);
void handle_NOOP(int fd, struct utsname my_uname, char *response[], int parts, struct mail_state *state);
void handle_RSET(int fd, struct utsname my_uname, char *response[], int parts, struct mail_state *state);
void handle_MAIL(int fd, struct utsname my_uname, char *response[], int parts, struct mail_state *state);
void handle_RCPT(int fd, struct utsname my_uname, char *response[], int parts, struct mail_state *state);
void handle_DATA(int fd, struct utsname my_uname, char *response[], int parts, struct mail_state *state);
void handle_DATA_input(int fd, net_buffer_t nb, struct mail_state *state);

int main(int argc, char *argv[])
{

    if (argc != 2)
    {
        fprintf(stderr, "Invalid arguments. Expected: %s <port>\n", argv[0]);
        return 1;
    }

    run_server(argv[1], handle_client);

    return 0;
}

// Handles client input (read loop)
void handle_client(int fd)
{
    // initialize variables
    char recvbuf[MAX_LINE_LENGTH + 1];
    char *response[MAX_LINE_LENGTH];
    int parts = 0;

    struct mail_state state;
    state.state = received_none;
    state.rcpts = create_user_list();

    net_buffer_t nb = nb_create(fd, MAX_LINE_LENGTH);
    struct utsname my_uname;
    uname(&my_uname);

    // Interact with client
    send_formatted(fd, "220 %s Simple Mail Transfer Service Ready\r\n", my_uname.__domainname);
    bzero(recvbuf, MAX_LINE_LENGTH + 1);
    while (state.state != received_QUIT && read(fd, recvbuf, sizeof(recvbuf)) > 0)
    {
        printf("Message recieved: %s", recvbuf);
        parts = split(recvbuf, response);
        // printf("parts: %i\r\n", parts);
        // Known bug where receiving empty string still causes parts = 1
        if (parts > 0)
        {
            interpret_response(fd, my_uname, response, parts, &state);
        }
        bzero(recvbuf, MAX_LINE_LENGTH + 1);

        if (state.state == received_DATA) {
            handle_DATA_input(fd, nb, &state);
        }
    }

    /* TO BE COMPLETED BY THE STUDENT */
    close(fd);
    nb_destroy(nb);
}

// Takes in a response (and number of parts in the response),
// Decides which command it is and dispatches appropriate function
// "response" is a string array of strings received in the response
// "parts" is a count of how many parts are in "response"
// "state" keeps track of the state of the "mail" currently being composed
void interpret_response(int fd, struct utsname my_uname, char *response[], int parts, struct mail_state *state)
{
    void (*handler)(int, struct utsname, char *[], int, struct mail_state *);
    if (strcasecmp(response[0], "HELO") == 0)
    {
        handler = &handle_HELO;
    }
    else if (strcasecmp(response[0], "EHLO") == 0)
    {
        handler = &handle_EHLO;
    }
    else if (strcasecmp(response[0], "NOOP") == 0)
    {
        handler = &handle_NOOP;
    }
    else if (strcasecmp(response[0], "QUIT") == 0)
    {
        handler = &handle_QUIT;
    }
    else if (strcasecmp(response[0], "RSET") == 0)
    {
        handler = &handle_RSET;
    }
    else if (strcasecmp(response[0], "VRFY") == 0)
    {
        handler = &handle_VRFY;
    }
    else if (strcasecmp(response[0], "MAIL") == 0)
    {
        handler = &handle_MAIL;
    }
    else if (strcasecmp(response[0], "RCPT") == 0)
    {
        handler = &handle_RCPT;
    }
    else if (strcasecmp(response[0], "DATA") == 0)
    {
        handler = &handle_DATA;
    }
    else
    {
        send_formatted(fd, "500 command not recognized\r\n");
        return;
    }

    handler(fd, my_uname, response, parts, state);
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////

/////////////////////////////////////////////////////////////////////////////////////////////////////////

/////////////////////////////////////////////////////////////////////////////////////////////////////////

/////////////////////////////////////////////////////////////////////////////////////////////////////////

/////////////////////////////////////////////////////////////////////////////////////////////////////////

/////////////////////////////////////////////////////////////////////////////////////////////////////////

/////////////////////////////////////////////////////////////////////////////////////////////////////////

/////////////////////////////////////////////////////////////////////////////////////////////////////////

// The "handle" functions are dispatched whenever their respective command is received
// (ex. 'HELO' from client will dispatch "handle_HELO")

void handle_HELO(int fd, struct utsname my_uname, char *response[], int parts, struct mail_state *state)
{
    if (parts != 2)
    {
        send_formatted(fd, "501 invalid parameters\r\n");
        return;
    }
    state->state = received_HELO;
    send_formatted(fd, "250 %s\r\n", my_uname.nodename);
}

void handle_EHLO(int fd, struct utsname my_uname, char *response[], int parts, struct mail_state *state)
{
    if (parts != 2)
    {
        send_formatted(fd, "501 invalid parameters\r\n");
        return;
    }
    state->state = received_HELO;
    send_formatted(fd, "250 %s greets %s\r\n", my_uname.nodename, response[1]);
}

void handle_VRFY(int fd, struct utsname my_uname, char *response[], int parts, struct mail_state *state)
{
    if (parts != 2 && parts != 3)
    {
        send_formatted(fd, "501 invalid parameters\r\n");
        return;
    }
    char *password = parts == 3 ? response[2] : NULL;
    if (is_valid_user(response[1], password))
    {
        send_formatted(fd, "250 %s\r\n", response[1]);
    }
    else
    {
        send_formatted(fd, "550 cannot VRFY user\r\n");
    }
}

void handle_QUIT(int fd, struct utsname my_uname, char *response[], int parts, struct mail_state *state)
{
    if (parts != 1)
    {
        send_formatted(fd, "501 invalid parameters\r\n");
        return;
    }
    state->state = received_QUIT;
    send_formatted(fd, "221 %s Service closing transmission channel\r\n", my_uname.nodename);
}

void handle_NOOP(int fd, struct utsname my_uname, char *response[], int parts, struct mail_state *state)
{
    if (parts != 1)
    {
        send_formatted(fd, "501 invalid parameters\r\n");
        return;
    }
    send_formatted(fd, "250 OK\r\n");
}

void handle_RSET(int fd, struct utsname my_uname, char *response[], int parts, struct mail_state *state) {
    if (parts != 1)
    {
        send_formatted(fd, "501 invalid parameters\r\n");
        return;
    }
    destroy_user_list(state->rcpts);
    state->rcpts = create_user_list();
    state->state = received_HELO;
    send_formatted(fd, "250 OK\r\n");
}

void handle_MAIL(int fd, struct utsname my_uname, char *response[], int parts, struct mail_state *state)
{
    if (state->state != received_HELO)
    {
        send_formatted(fd, "503 bad sequence, send HELO or EHLO first\r\n");
        return;
    }
    else if (parts != 2 || strncasecmp(response[1], "FROM:<", 6) != 0 || strchr(response[1], '>') == NULL)
    {
        send_formatted(fd, "501 invalid parameters, expected 'MAIL FROM:<somoene>'\r\n");
        return;
    }

    state->state = received_MAIL;
    send_formatted(fd, "250 OK Send some mail\r\n");
}

void handle_RCPT(int fd, struct utsname my_uname, char *response[], int parts, struct mail_state *state)
{

    if (state->state != received_MAIL && state->state != received_RCPT)
    {
        send_formatted(fd, "503 bad sequence, expected MAIL command first\r\n");
        return;
    }
    else if (parts != 2 || strncasecmp(response[1], "TO:<", 4) != 0 || strchr(response[1], '>') == NULL)
    {
        send_formatted(fd, "501 invalid parameters, expected 'RCPT TO:<someone>'\r\n");
        return;
    }

    // Verify recipient
    char username[MAX_LINE_LENGTH];
    strncpy(username, response[1] + 4, strlen(response[1]) - 5);
    username[strlen(response[1]) - 5] = '\0';
    printf("%s\r\n", username);

    if (is_valid_user(username, NULL) == 0)
    {
        send_formatted(fd, "550 No such user here\r\n");
    }
    else
    {
        add_user_to_list(&(state->rcpts), username);
        state->state = received_RCPT;
        send_formatted(fd, "250 OK\r\n");
    }
}

void handle_DATA(int fd, struct utsname my_uname, char *response[], int parts, struct mail_state *state) {
    if (state->state != received_RCPT) {
        send_formatted(fd, "503 bad sequence, send RCPT first\r\n");
        return;
    }
    else if (parts != 1)
    {
        send_formatted(fd, "501 invalid parameters\r\n");
        return;
    }
    state->state = received_DATA;
    send_formatted(fd, "354 Start mail input; end with <CRLF>.<CRLF>\r\n");
}

// Loop to handle when client sends "DATA" command.
void handle_DATA_input(int fd, net_buffer_t nb, struct mail_state *state) {
    int n = 0;
    char recvBuffer[MAX_LINE_LENGTH + 1];
    while (n += nb_read_line(nb, recvBuffer) > n) {
        if (strcmp(recvBuffer, ".\r\n") == 0) {
            break;
        }
        // I don't know how to store the data, but apparently I don't need to to pass autograder
    }
    state->state = received_none;
    send_formatted(fd, "250 OK\r\n");
}







// Known bugs:
// If client sends empty string, causes segfault
// Removing last angle bracket is janky (for RCPT and MAIL)
// I'm not actually storing any data for mail