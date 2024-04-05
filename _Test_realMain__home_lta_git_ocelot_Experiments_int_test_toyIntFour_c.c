#include <stdlib.h>
#include <check.h>
START_TEST(ocelot_testcase4)
{
double __val0 = 73;
double __val1 = 20;
double __val2 = 404978376;
double __val3 = -1107221642;



int __arg0 = __val0;
int __arg1 = __val1;
numOfCall_functionA = __val2;
numOfCall_functionB = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


START_TEST(ocelot_testcase3)
{
double __val0 = -76;
double __val1 = -32;
double __val2 = 350458101;
double __val3 = 1608472809;



int __arg0 = __val0;
int __arg1 = __val1;
numOfCall_functionA = __val2;
numOfCall_functionB = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


START_TEST(ocelot_testcase2)
{
double __val0 = 30;
double __val1 = -19;
double __val2 = 801716785;
double __val3 = 2143131922;



int __arg0 = __val0;
int __arg1 = __val1;
numOfCall_functionA = __val2;
numOfCall_functionB = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


START_TEST(ocelot_testcase5)
{
double __val0 = 12;
double __val1 = 13;
double __val2 = 524032914;
double __val3 = 1008171604;



int __arg0 = __val0;
int __arg1 = __val1;
numOfCall_functionA = __val2;
numOfCall_functionB = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


START_TEST(ocelot_testcase1)
{
double __val0 = -11;
double __val1 = 97;
double __val2 = 143580949;
double __val3 = -338040910;



int __arg0 = __val0;
int __arg1 = __val1;
numOfCall_functionA = __val2;
numOfCall_functionB = __val3;
realMain(__arg0,__arg1);

/* REPLACE THE ASSERTION BELOW */
ck_assert_str_eq("OK", "OK");
}
END_TEST


Suite * ocelot_generated_109ac4e3(void)
{
Suite *s;
TCase *temp_tc;

s = suite_create("ocelot_generated_109ac4e3");

temp_tc = tcase_create("ocelot_testcase4");
tcase_add_test(temp_tc, ocelot_testcase4);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase3");
tcase_add_test(temp_tc, ocelot_testcase3);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase2");
tcase_add_test(temp_tc, ocelot_testcase2);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase5");
tcase_add_test(temp_tc, ocelot_testcase5);
suite_add_tcase(s, temp_tc);

temp_tc = tcase_create("ocelot_testcase1");
tcase_add_test(temp_tc, ocelot_testcase1);
suite_add_tcase(s, temp_tc);

return s;
}

int main(void) {
int number_failed;
Suite *s;
SRunner *sr;

s = ocelot_generated_109ac4e3();
sr = srunner_create(s);

srunner_run_all(sr, CK_NORMAL);
number_failed = srunner_ntests_failed(sr);
srunner_free(sr);
return (number_failed == 0) ? EXIT_SUCCESS : EXIT_FAILURE;
}