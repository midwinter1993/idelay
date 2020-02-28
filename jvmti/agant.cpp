#include <jvmti.h>
#include <stdio.h>
#include <atomic>
#include <string>

#include <iostream>

#include "utils.h"

static bool g_opt_dump_thread = false;
static int g_opt_num_spot_threads = 3;

static bool is_number(const std::string& s) {
    return !s.empty() && std::find_if(s.begin(),
        s.end(), [](unsigned char c) { return !std::isdigit(c); }) == s.end();
}

static void parse_sub_option(const std::string &sub_opt) {
    size_t pos = sub_opt.find(":");
    if (pos == std::string::npos) {
        return;
    }
    std::string key = sub_opt.substr(0, pos);
    std::string value = sub_opt.substr(pos + 1);

    if (key == "dump_thread" && value == "true") {
        g_opt_dump_thread = true;
        fprintf(stderr, "[ ! set dump thread ]\n");
        return;
    }
    if (key == "num_spot_threads") {
        if (!is_number(value)) {
            return;
        }
        g_opt_num_spot_threads = std::stoi(value);
        fprintf(stderr, "[ ! #Thread to start working %d ]\n", g_opt_num_spot_threads);
    }
}

static void parse_options(const char *options) {
    if (!options) {
        return;
    }
    std::string delimiter = ",";

    std::string buf{options};
    if (buf.back() != ',') {
        buf.push_back(','); // Append a delimiter for while loop
    }

    size_t pos = 0;
    std::string sub_opt;
    while ((pos = buf.find(delimiter)) != std::string::npos) {
        sub_opt = buf.substr(0, pos);
        // fprintf(stderr, "%s\n", sub_opt.c_str());
        parse_sub_option(sub_opt);
        buf.erase(0, pos + delimiter.length());
    }
}

static std::atomic_int g_num_threads{0};

static void invodeStateMethod(JNIEnv *jni_env, const char *name) {
    static const char *STATE_CLASS_NAME = "io/github/midwinter1993/State";
    jclass cls = jni_env->FindClass(STATE_CLASS_NAME);
    if (cls == nullptr) {
        fprintf(stderr, "ERROR: NOT found class [%s]!\n", STATE_CLASS_NAME);
        return;
    }

    //
    // The callee method is with signature void()
    //
    jmethodID method = jni_env->GetStaticMethodID(cls, name, "()V");
    if(method == nullptr) {
        fprintf(stderr,"ERROR: NOT found method void State.%s:()V!\n", name);
    } else {
        jni_env->CallStaticVoidMethod(cls, method);
    }
}

void JNICALL ThreadStart(jvmtiEnv* jvmti, JNIEnv* jni_env, jthread thread) {
    jvmtiError err_code;

    jvmtiThreadInfo info;
    err_code = jvmti->GetThreadInfo(thread, &info);
    if (err_code == 0) {
        int num = g_num_threads.fetch_add(1, std::memory_order_seq_cst) + 1;

        if (g_opt_dump_thread) {
            fprintf(stderr, "Thread started: %s #%d\n", info.name, num);

            jvmtiThreadGroupInfo group_info;
            err_code = jvmti->GetThreadGroupInfo(info.thread_group, &group_info);
            CHECK_ERROR("Get thread group failure");
            if (!err_code) {
                fprintf(stderr, "Thread group: %s\n", group_info.name);
            }
        }

        //
        // An approximated for multiple thread created by applications
        // Main thread, Signal Dispatcher, new thread
        //
        if (num == g_opt_num_spot_threads) {
            invodeStateMethod(jni_env, "startWork");
        }
    }
}

void JNICALL ThreadEnd(jvmtiEnv* jvmti, JNIEnv* env, jthread thread) {
    // jvmtiThreadInfo info;
    // jvmtiError err_code = jvmti->GetThreadInfo(thread, &info);
    // if (err_code == 0) {
    //     int x = g_num_threads.fetch_add(-1, std::memory_order_seq_cst);
    //     fprintf(stderr, "Thread end: %s %d\n", info.name, x-1);
    // }
}

//
// Provide a global variable for easy access
//
jvmtiEnv* g_jvmti = NULL;

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM* vm, char* options, void* reserved) {

    parse_options(options);

    vm->GetEnv((void**) &g_jvmti, JVMTI_VERSION_1_0);

    jvmtiEnv *jvmti = g_jvmti;
    utils::init(jvmti, options);

    //
    // Register our capabilities
    //
    jvmtiCapabilities cap= {0};

    // cap.can_generate_all_class_hook_events = 1;
    // cap.can_generate_field_modification_events = 1;
    // cap.can_generate_field_access_events = 1;
    // cap.can_generate_method_entry_events = 1;
    // cap.can_get_source_file_name = 1;
    // cap.can_get_line_numbers = 1;

    jvmtiError err_code = jvmti->AddCapabilities(&cap);
    CHECK_ERROR("Add capability failure");

    //
    // Register callbacks
    //
    jvmtiEventCallbacks callbacks = {0};

    callbacks.ThreadStart = ThreadStart;
    callbacks.ThreadEnd = ThreadEnd;

    jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    CHECK_ERROR("Set callbacks failure");

    //
    // Register for events
    //
    jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_START, NULL);
    jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_END, NULL);
    CHECK_ERROR("Set event notifications failure");

    return 0;
}

JNIEXPORT void JNICALL Agent_OnUnload(JavaVM* vm) {
    utils::fini();
}

