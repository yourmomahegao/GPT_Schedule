import requests
import json
from bs4 import BeautifulSoup

SITE = "https://guspoliteh.ru/studentu/raspisanie-zanyatiy/"

def getGroups():
    r = requests.get(SITE)
    site_text = r.text

    group_ids = []
    teachers_ids = []
    group_names = []
    teachers_names = []

    soup = BeautifulSoup(site_text, 'html.parser')
    html_all_options = soup.find_all("option")

    for option in html_all_options:

        if option.get_text() == "Выберите группу":
            option_change = True
        elif option.get_text() == "Выберите преподавателя":
            option_change = False

        if option_change:
            group_ids.append(option['value'])
            group_names.append(option.get_text())
        else:
            teachers_ids.append(option['value'])
            teachers_names.append(option.get_text())


    j = json.dumps({'group_ids': group_ids, 'teachers_ids': teachers_ids, 'group_names': group_names, 'teachers_names': teachers_names}, ensure_ascii=False)

    return j

def findGroupID(name: str):
    global groups, teachers, group_names, teachers_names

    if groups is None or teachers is None or group_names is None or teachers_names is None:
        return False, False

    for i in range(0, len(group_names)):
        if name == group_names[i]:
            return groups[i], False

    for i in range(0, len(teachers_names)):
        if name == teachers_names[i]:
            return teachers[i], True


    return False, False


def getSchedule(group_id: int, date: str, is_teacher: bool):

    if is_teacher:
        r = requests.post(SITE, data={'id': group_id, 'date': date, 'modal1': True})
    else:
        r = requests.post(SITE, data={'id': group_id, 'date': date, 'modal2': True})

    soup = BeautifulSoup(r.text, 'html.parser')
    lessons_raw = soup.find_all("div", {"class": "rlarge"})
    lessons = []

    teachers_raw = soup.find_all("div", {"class": "rsmall"})
    teachers = []

    for i in range(2, len(lessons_raw)):
        lr = lessons_raw[i].get_text().strip()

        if lr == None:
            pass
        elif lr == '':
            lessons.append('')
        else:
            lessons.append(lr)

    for t in teachers_raw:
        tr = teachers.append(t.get_text().strip())

        if tr == None:
            pass
        elif tr == '':
            lessons.append('Преподаватель не установлен')
        else:
            lessons.append(tr)

    if len(lessons) == 0:
        for i in range(0, 4):
            lessons.append('')

    if len(teachers) == 0:
        for i in range(0, 4):
            teachers.append('Преподаватель не установлен')

    return json.dumps({"lessons": lessons, "teachers": teachers}, ensure_ascii=True)

if __name__ == "__main__":
    print(getSchedule(69, "2023-09-13", False))