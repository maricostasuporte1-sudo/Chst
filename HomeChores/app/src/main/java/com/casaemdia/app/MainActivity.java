package com.casaemdia.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(246, 247, 249);
    private static final int CARD = Color.WHITE;
    private static final int TEXT = Color.rgb(28, 32, 36);
    private static final int MUTED = Color.rgb(104, 112, 120);
    private static final int ACCENT = Color.rgb(23, 107, 91);
    private static final int DANGER = Color.rgb(184, 54, 54);

    private Store store;
    private LinearLayout content;
    private final List<Button> navButtons = new ArrayList<>();
    private int section = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new Store(this);
        NotificationScheduler.ensureChannel(this);
        NotificationScheduler.scheduleNext(this);
        requestNotificationPermission();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(18), dp(18), dp(22));
        scroll.addView(content);

        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(dp(8), dp(8), dp(8), dp(8));
        nav.setBackgroundColor(Color.WHITE);

        String[] labels = {"Hoje", "Escala", "Pessoas", "Ajustes"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            Button b = new Button(this);
            b.setText(labels[i]);
            b.setTextSize(12);
            b.setAllCaps(false);
            b.setMinHeight(dp(48));
            b.setOnClickListener(v -> showSection(index));
            navButtons.add(b);
            nav.addView(b, new LinearLayout.LayoutParams(0, dp(52), 1f));
        }
        root.addView(nav);
        setContentView(root);
        showSection(0);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 22);
        }
    }

    private void showSection(int index) {
        section = index;
        content.removeAllViews();

        for (int i = 0; i < navButtons.size(); i++) {
            Button b = navButtons.get(i);
            b.setTextColor(i == index ? Color.WHITE : MUTED);
            b.setBackground(i == index ? rounded(ACCENT, 14) : rounded(Color.TRANSPARENT, 14));
        }

        if (index == 0) renderToday();
        else if (index == 1) renderSchedule();
        else if (index == 2) renderPeople();
        else renderSettings();
    }

    private void addHeader(String title, String subtitle) {
        content.addView(text(title, 28, TEXT, true));
        TextView sub = text(subtitle, 14, MUTED, false);
        LinearLayout.LayoutParams p = matchWrap();
        p.topMargin = dp(4);
        p.bottomMargin = dp(18);
        content.addView(sub, p);
    }

    private void renderToday() {
        String date = new SimpleDateFormat("EEEE, d 'de' MMMM", new Locale("pt", "BR"))
                .format(Calendar.getInstance().getTime());
        addHeader("Casa em Dia", capitalize(date));

        List<Task> tasks = store.getTodayTasks();
        int done = 0;
        for (Task t : tasks) if (store.isDone(t.id)) done++;

        LinearLayout summary = card();
        summary.addView(text("HOJE", 12, MUTED, true));
        summary.addView(text(
                tasks.isEmpty() ? "Tudo livre por aqui" :
                        tasks.size() + " tarefa" + (tasks.size() == 1 ? "" : "s"),
                22, TEXT, true), top(dp(4)));
        summary.addView(text(
                tasks.isEmpty() ? "Nenhuma tarefa programada para hoje." :
                        done + " concluída" + (done == 1 ? "" : "s") + " de " + tasks.size(),
                14, MUTED, false), top(dp(4)));
        content.addView(summary, bottom(dp(16)));

        if (tasks.isEmpty()) {
            content.addView(text(
                    "Vá em Escala para adicionar tarefas como lavar a louça, varrer o chão, estender roupa, lavar roupa ou tirar o lixo.",
                    15, MUTED, false));
            Button create = primary("Criar primeira tarefa");
            create.setOnClickListener(v -> showSection(1));
            content.addView(create, top(dp(16)));
            return;
        }

        for (Task task : tasks) {
            LinearLayout row = card();
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            CheckBox check = new CheckBox(this);
            check.setChecked(store.isDone(task.id));
            row.addView(check, new LinearLayout.LayoutParams(dp(48), dp(48)));

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            info.addView(text(task.title, 17, TEXT, true));
            info.addView(text(task.person, 13, ACCENT, true), top(dp(3)));
            row.addView(info, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            check.setOnCheckedChangeListener((buttonView, isChecked) -> {
                store.setDone(task.id, isChecked);
                showSection(0);
            });

            content.addView(row, bottom(dp(10)));
        }
    }

    private void renderSchedule() {
        addHeader("Escala da casa", "Defina quem faz cada tarefa e em quais dias.");

        Button add = primary("+ Nova tarefa");
        add.setOnClickListener(v -> showAddTaskDialog());
        content.addView(add, bottom(dp(16)));

        List<Task> tasks = store.getTasks();
        if (tasks.isEmpty()) {
            content.addView(text("Ainda não há tarefas na escala.", 15, MUTED, false));
            return;
        }

        for (Task task : tasks) {
            LinearLayout c = card();
            c.addView(text(task.title, 17, TEXT, true));
            c.addView(text(task.person + "  •  " + daysText(task.daysMask),
                    13, MUTED, false), top(dp(4)));

            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);

            Button edit = secondary("Editar");
            edit.setOnClickListener(v -> showEditTaskDialog(task));
            actions.addView(edit, new LinearLayout.LayoutParams(
                    0, dp(48), 1f));

            Button del = secondary("Excluir");
            del.setTextColor(DANGER);
            del.setOnClickListener(v -> confirmDeleteTask(task));
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                    0, dp(48), 1f);
            deleteParams.leftMargin = dp(8);
            actions.addView(del, deleteParams);

            c.addView(actions, top(dp(10)));
            content.addView(c, bottom(dp(10)));
        }
    }

    private void showAddTaskDialog() {
        List<String> people = store.getPeople();
        if (people.isEmpty()) {
            Toast.makeText(this, "Adicione uma pessoa primeiro.", Toast.LENGTH_SHORT).show();
            showSection(2);
            return;
        }

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);

        EditText taskName = new EditText(this);
        taskName.setHint("Ex.: lavar a louça");
        taskName.setTextSize(17);
        box.addView(taskName, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        box.addView(text("Responsável", 13, MUTED, true), top(dp(12)));

        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, people));
        box.addView(spinner, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        box.addView(text("Dias da semana", 13, MUTED, true), top(dp(12)));

        String[] dayNames = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"};
        LinearLayout days = new LinearLayout(this);
        days.setOrientation(LinearLayout.VERTICAL);
        CheckBox[] checks = new CheckBox[7];

        for (int i = 0; i < 7; i++) {
            checks[i] = new CheckBox(this);
            checks[i].setText(dayNames[i]);
            checks[i].setTextSize(15);
            if (i < 5) checks[i].setChecked(true);
            days.addView(checks[i]);
        }
        box.addView(days);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Nova tarefa")
                .setView(box)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null)
                .create();

        dialog.setOnShowListener(ignored ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String title = taskName.getText().toString().trim();
                    if (title.isEmpty()) {
                        taskName.setError("Digite o nome da tarefa.");
                        return;
                    }

                    int mask = 0;
                    for (int i = 0; i < 7; i++) if (checks[i].isChecked()) mask |= (1 << i);
                    if (mask == 0) {
                        Toast.makeText(this, "Escolha pelo menos um dia.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Task> tasks = store.getTasks();
                    tasks.add(new Task(
                            String.valueOf(System.nanoTime()),
                            title,
                            String.valueOf(spinner.getSelectedItem()),
                            mask));
                    store.saveTasks(tasks);
                    NotificationScheduler.scheduleNext(this);
                    dialog.dismiss();
                    showSection(1);
                }));

        dialog.show();
    }

    private void showEditTaskDialog(Task task) {
        List<String> people = store.getPeople();
        if (people.isEmpty()) {
            Toast.makeText(this, "Adicione uma pessoa primeiro.", Toast.LENGTH_SHORT).show();
            showSection(2);
            return;
        }

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);

        EditText taskName = new EditText(this);
        taskName.setHint("Ex.: lavar a louça");
        taskName.setText(task.title);
        taskName.setTextSize(17);
        box.addView(taskName, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        box.addView(text("Responsável", 13, MUTED, true), top(dp(12)));

        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, people));
        int selectedPerson = 0;
        for (int i = 0; i < people.size(); i++) {
            if (people.get(i).equals(task.person)) {
                selectedPerson = i;
                break;
            }
        }
        spinner.setSelection(selectedPerson);
        box.addView(spinner, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        box.addView(text("Dias da semana", 13, MUTED, true), top(dp(12)));

        String[] dayNames = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"};
        LinearLayout days = new LinearLayout(this);
        days.setOrientation(LinearLayout.VERTICAL);
        CheckBox[] checks = new CheckBox[7];

        for (int i = 0; i < 7; i++) {
            checks[i] = new CheckBox(this);
            checks[i].setText(dayNames[i]);
            checks[i].setTextSize(15);
            checks[i].setChecked((task.daysMask & (1 << i)) != 0);
            days.addView(checks[i]);
        }
        box.addView(days);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Editar tarefa")
                .setView(box)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null)
                .create();

        dialog.setOnShowListener(ignored ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String title = taskName.getText().toString().trim();
                    if (title.isEmpty()) {
                        taskName.setError("Digite o nome da tarefa.");
                        return;
                    }

                    int mask = 0;
                    for (int i = 0; i < 7; i++) if (checks[i].isChecked()) mask |= (1 << i);
                    if (mask == 0) {
                        Toast.makeText(this, "Escolha pelo menos um dia.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String person = String.valueOf(spinner.getSelectedItem());
                    List<Task> tasks = store.getTasks();
                    for (int i = 0; i < tasks.size(); i++) {
                        Task current = tasks.get(i);
                        if (current.id.equals(task.id)) {
                            tasks.set(i, new Task(task.id, title, person, mask));
                            break;
                        }
                    }

                    store.saveTasks(tasks);
                    NotificationScheduler.scheduleNext(this);
                    dialog.dismiss();
                    showSection(1);
                }));

        dialog.show();
    }

    private void confirmDeleteTask(Task task) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir tarefa?")
                .setMessage(task.title + " será removida da escala.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (d, w) -> {
                    List<Task> kept = new ArrayList<>();
                    for (Task t : store.getTasks()) if (!t.id.equals(task.id)) kept.add(t);
                    store.saveTasks(kept);
                    NotificationScheduler.scheduleNext(this);
                    showSection(1);
                })
                .show();
    }

    private void renderPeople() {
        addHeader("Pessoas", "Cadastre quem participa da rotina da casa.");

        LinearLayout addRow = new LinearLayout(this);
        addRow.setOrientation(LinearLayout.HORIZONTAL);

        EditText input = new EditText(this);
        input.setHint("Nome");
        input.setSingleLine(true);
        addRow.addView(input, new LinearLayout.LayoutParams(0, dp(54), 1f));

        Button add = primary("Adicionar");
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(dp(112), dp(48));
        ap.leftMargin = dp(8);
        addRow.addView(add, ap);
        content.addView(addRow, bottom(dp(16)));

        add.setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) return;

            List<String> people = store.getPeople();
            for (String p : people) {
                if (p.equalsIgnoreCase(name)) {
                    Toast.makeText(this, "Essa pessoa já existe.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            people.add(name);
            store.savePeople(people);
            showSection(2);
        });

        for (String person : store.getPeople()) {
            LinearLayout row = card();
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            row.addView(text(person, 17, TEXT, true),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            Button remove = secondary("Remover");
            remove.setTextColor(DANGER);
            remove.setOnClickListener(v -> removePerson(person));
            row.addView(remove);

            content.addView(row, bottom(dp(10)));
        }
    }

    private void removePerson(String person) {
        for (Task t : store.getTasks()) {
            if (t.person.equals(person)) {
                Toast.makeText(this,
                        "Essa pessoa ainda tem tarefas na escala.",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        List<String> people = store.getPeople();
        people.remove(person);
        store.savePeople(people);
        showSection(2);
    }

    private void renderSettings() {
        addHeader("Ajustes", "Controle o horário dos lembretes no celular.");

        LinearLayout c = card();
        c.addView(text("Lembrete diário", 17, TEXT, true));
        c.addView(text(String.format(
                        Locale.getDefault(),
                        "Todos os dias às %02d:%02d",
                        store.getHour(), store.getMinute()),
                14, MUTED, false), top(dp(4)));

        Button time = secondary("Alterar horário");
        time.setOnClickListener(v ->
                new TimePickerDialog(
                        this,
                        (view, hour, minute) -> {
                            store.setNotifyTime(hour, minute);
                            NotificationScheduler.scheduleNext(this);
                            showSection(3);
                        },
                        store.getHour(),
                        store.getMinute(),
                        true).show());
        c.addView(time, top(dp(12)));

        Button test = primary("Testar notificação");
        test.setOnClickListener(v -> {
            requestNotificationPermission();
            NotificationScheduler.showNow(
                    this, "Casa em Dia", "As notificações estão funcionando.");
        });
        c.addView(test, top(dp(10)));

        content.addView(c, bottom(dp(14)));
        content.addView(text(
                "As tarefas ficam salvas neste aparelho. O app envia um resumo das tarefas do dia no horário escolhido.",
                14, MUTED, false));
    }

    private String daysText(int mask) {
        String[] names = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if ((mask & (1 << i)) != 0) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(names[i]);
            }
        }
        return sb.toString();
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        c.setBackground(rounded(CARD, 18));
        return c;
    }

    private Button primary(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setBackground(rounded(ACCENT, 14));
        b.setMinHeight(dp(48));
        return b;
    }

    private Button secondary(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(ACCENT);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setBackground(rounded(Color.rgb(236, 241, 239), 12));
        b.setMinHeight(dp(44));
        return b;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(size);
        tv.setTextColor(color);
        tv.setLineSpacing(0, 1.12f);
        if (bold) tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return tv;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(dp(radiusDp));
        return gd;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams top(int margin) {
        LinearLayout.LayoutParams p = matchWrap();
        p.topMargin = margin;
        return p;
    }

    private LinearLayout.LayoutParams bottom(int margin) {
        LinearLayout.LayoutParams p = matchWrap();
        p.bottomMargin = margin;
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (content != null) showSection(section);
    }
}
