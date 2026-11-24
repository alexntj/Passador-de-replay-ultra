package com.Privvideo.app;

interface IPrivilegedService {
    void destroy();
    void exit();
    String execute(String command);
}
