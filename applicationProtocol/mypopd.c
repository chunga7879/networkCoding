#include "netbuffer.h"
#include "mailuser.h"
#include "server.h"

#include <stdio.h>
#include <sys/utsname.h>

#include <stdlib.h>
#include <unistd.h>
#include <ctype.h>

#define MAX_LINE_LENGTH 1024

static void handle_client(int fd);
int command_int(char line[]);

// update
void update(char count[], char size[], mail_list_t mail_list)
{
    sprintf(count, "%u", get_mail_count(mail_list, 0));
    sprintf(size, "%zu", get_mail_list_size(mail_list));
}

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

void handle_client(int fd)
{

    // variables
    //    char recvbuf[MAX_LINE_LENGTH + 1];
    char user[MAX_USERNAME_SIZE];
    char pass[MAX_PASSWORD_SIZE];

    mail_list_t mail_list = NULL;

    int state = 0; // 1: AUTHORIZATION 2: TRANSACTION
    int res;       // for error checking from send_formatted
    char count[10], size[20];

    // greeting
    struct utsname my_uname;
    uname(&my_uname);

    res = send_formatted(fd, "+OK %s POP3 server ready\r\n", my_uname.nodename);

    if (res == -1)
    {
        return;
    }

    net_buffer_t nb = nb_create(fd, MAX_LINE_LENGTH);

    while (1)
    {
        int command = 0;
        unsigned int countNumMail;
        char line[MAX_LINE_LENGTH];
        // RETURN number of bytes in the read line
        int res = nb_read_line(nb, line);

        // check whether res is correctly received from read line
        if (res == 0 || res == -1)
        {
            if (res == 0)
            {
                send_formatted(fd, "+OK the connection was terminated properly\r\n");
            }
            if (res == -1)
            {
                send_formatted(fd, "-ERR the connection was terminated abruptly OR another unknown error is found\r\n");
            }

            if (mail_list != NULL)
            {
                reset_mail_list_deleted_flag(mail_list);
                destroy_mail_list(mail_list);
            }

            line[0] = '\0';
            break;
        }

        // argument and parameter
        char parameter[MAX_LINE_LENGTH];
        char *response[MAX_LINE_LENGTH];
        int parts = split(line, response);
        command = command_int(response[0]);
        if (parts > 1)
        {
            strcpy(parameter, response[1]);
        }

        // USER
        if (command == 1 && state == 0)
        {
            if (strlen(parameter) < 1)
            {
                res = send_formatted(fd, "-ERR user name is not accepted as parameter\r\n");
            }
            else if (is_valid_user(parameter, NULL) == 0)
            {
                res = send_formatted(fd, "-ERR never heard of mailbox name\r\n");
            }
            else
            {
                res = send_formatted(fd, "+OK name is a valid mailbox\r\n");
                strcpy(user, parameter);
                parameter[0] = '\0';
                state += 1;
            }

            if (res == -1)
            {
                break;
            }
            continue;
        }
        // PASS
        else if (command == 2 && (state == 0 || state == 1))
        {
            if (state == 0)
            {
                res = send_formatted(fd, "-ERR no user name is provided\r\n");
            }
            else if (strlen(parameter) < 1)
            {
                res = send_formatted(fd, "-ERR password is not accepted as parameter\r\n");
            }
            else if (is_valid_user(user, parameter) == 0)
            {
                res = send_formatted(fd, "-ERR invalid password\r\n");
                state = 0;
            }
            else
            {
                mail_list = load_user_mail(user);
                update(count, size, mail_list);
                countNumMail = get_mail_count(mail_list, 0);

                res = send_formatted(fd, "+OK maildrop has %s messages (%s octets)\r\n", count, size);
                if (res == -1)
                {
                    destroy_mail_list(mail_list);
                    break;
                }
                strcpy(pass, parameter);
                state += 1;
            }

            if (res == -1)
            {
                break;
            }
            continue;
        }
        // QUIT
        else if (command == 9)
        {
            if (state == 2)
            {
                destroy_mail_list(mail_list);
            }

            send_formatted(fd, "+OK %s POP3 server signing off \r\n", my_uname.nodename);
            break;
        }

        // Transaction State

        // STAT
        else if (state == 2 && command == 3)
        {
            update(count, size, mail_list);
            res = send_formatted(fd, "+OK %s %s\r\n", count, size);
            if (res == -1)
            {
                destroy_mail_list(mail_list);
                break;
            }
            continue;
        }

        // LIST
        else if (state == 2 && command == 4 && (parts == 2 || parts == 1))
        {
            update(count, size, mail_list);
            if (parts == 2)
            {
                unsigned int number = (unsigned)strtol(parameter, NULL, 10);
                if (number == 0)
                {
                    res = send_formatted(fd, "-ERR Invalid arguments\r\n");
                }
                else
                {
                    mail_item_t mail_item = get_mail_item(mail_list, number - 1);
                    if (mail_item == NULL)
                    {
                        res = send_formatted(fd, "-ERR no such message\r\n");
                    }
                    else
                    {
                        char mail_size[10] = "";
                        sprintf(mail_size, "%zu", get_mail_item_size(mail_item));
                        res = send_formatted(fd, "+OK %s %s\r\n", parameter, mail_size);
                    }
                }
            }
            else if (parts == 1)
            {
                res = send_formatted(fd, "+OK %s messages (%s octets)\r\n", count, size);
                if (res == -1)
                {
                    destroy_mail_list(mail_list);
                    break;
                }

                int nMail = get_mail_count(mail_list, 1);
                for (int i = 0; i < nMail; i++)
                {
                    mail_item_t mail_item = get_mail_item(mail_list, i);
                    if (mail_item != NULL)
                    {
                        char item_num[10] = "";
                        char mail_size[10] = "";
                        sprintf(item_num, "%u", i + 1);
                        sprintf(mail_size, "%zu", get_mail_item_size(mail_item));
                        res = send_formatted(fd, "%s %s\r\n", item_num, mail_size);
                        if (res == -1)
                        {
                            destroy_mail_list(mail_list);
                            nb_destroy(nb);
                            return;
                        }
                    }
                }
                res = send_formatted(fd, ".\r\n");
            }
            if (res == -1)
            {
                destroy_mail_list(mail_list);
                break;
            }
            continue;
        }

        // RETR
        else if (state == 2 && command == 5)
        {
            update(count, size, mail_list);
            if (strlen(parameter) < 1)
            {
                res = send_formatted(fd, "-ERR no argument accepted\r\n");
            }
            else
            {
                unsigned int msgNum = (unsigned int)strtol(parameter, NULL, 10);
                if (msgNum == 0)
                {
                    res = send_formatted(fd, "-ERR Invalid argument\r\n");
                }
                else
                {
                    mail_item_t mail = get_mail_item(mail_list, msgNum - 1);
                    if (mail == NULL)
                    {
                        res = send_formatted(fd, "-ERR no such message\r\n");
                    }
                    else
                    {
                        char size[10] = "";
                        sprintf(size, "%zu", get_mail_item_size(mail));
                        res = send_formatted(fd, "+OK %s octets\r\n", size);
                        if (res == -1)
                        {
                            destroy_mail_list(mail_list);
                            break;
                        }
                        FILE *email_message = get_mail_item_contents(mail);

                        if (email_message == NULL)
                        {
                            res = send_formatted(fd, "-ERR no such message\r\n");
                            if (res == -1)
                            {
                                break;
                            }
                            continue;
                        }

                        char message[MAX_LINE_LENGTH];

                        while (fgets(message, MAX_LINE_LENGTH, email_message) != NULL)
                        {
                            res = send_formatted(fd, "%s", message);
                            if (res == -1)
                            {
                                nb_destroy(nb);
                                return;
                            }
                        }

                        res = send_formatted(fd, ".\r\n");
                        fclose(email_message);
                    }
                }
            }

            if (res == -1)
            {
                destroy_mail_list(mail_list);
                break;
            }
            continue;
        }

        // DELE
        else if (state == 2 && command == 6)
        {
            if (strlen(parameter) < 1)
            {
                res = send_formatted(fd, "-ERR no argument accepted\r\n");
            }
            else
            {
                unsigned int msgNum = (unsigned int)strtol(parameter, NULL, 10);
                if (msgNum == 0)
                {
                    res = send_formatted(fd, "-ERR invalid argument accepted\r\n");
                }
                else if (msgNum > countNumMail)
                {
                    res = send_formatted(fd, "-ERR no such message\r\n");
                }
                else
                {
                    mail_item_t mail = get_mail_item(mail_list, msgNum - 1);
                    if (mail == NULL)
                    {
                        res = send_formatted(fd, "-ERR no such message existed\r\n");
                    }
                    else
                    {
                        mark_mail_item_deleted(mail);
                        res = send_formatted(fd, "+OK message %s deleted\r\n", parameter);
                    }
                }
            }

            if (res == -1)
            {
                destroy_mail_list(mail_list);
                break;
            }
            continue;
        }

        // RSET
        else if (state == 2 && command == 7)
        {
            char rm[10] = "";
            sprintf(rm, "%u", reset_mail_list_deleted_flag(mail_list));

            update(count, size, mail_list);
            res = send_formatted(fd, "+OK %s messages recovered\r\n", rm);

            if (res == -1)
            {
                destroy_mail_list(mail_list);
                break;
            }
            continue;
        }

        // NOOP
        else if (state == 2 && command == 8)
        {
            res = send_formatted(fd, "+OK\r\n");
            if (res == -1)
            {
                destroy_mail_list(mail_list);
                break;
            }
            continue;
        }

        // Unrecognized command
        else
        {
            res = send_formatted(fd, "-ERR invalid command\r\n");
            if (state == 1) {
                state = 0;
            }
            if (res == -1)
            {
                break;
            }
        }
    }
    /* TO BE COMPLETED BY THE STUDENT */
    close(fd);
    nb_destroy(nb);
}

// return the integer result by comparing command from command line
int command_int(char line[])
{

    if (strcasecmp(line, "USER") == 0)
    {
        return 1;
    }
    else if (strcasecmp(line, "PASS") == 0)
    {
        return 2;
    }
    else if (strcasecmp(line, "STAT") == 0)
    {
        return 3;
    }
    else if (strcasecmp(line, "LIST") == 0)
    {
        return 4;
    }
    else if (strcasecmp(line, "RETR") == 0)
    {
        return 5;
    }
    else if (strcasecmp(line, "DELE") == 0)
    {
        return 6;
    }
    else if (strcasecmp(line, "RSET") == 0)
    {
        return 7;
    }
    else if (strcasecmp(line, "NOOP") == 0)
    {
        return 8;
    }
    else if (strcasecmp(line, "QUIT") == 0)
    {
        return 9;
    }

    return 0;
}