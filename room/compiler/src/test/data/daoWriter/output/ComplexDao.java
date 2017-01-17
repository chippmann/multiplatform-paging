package foo.bar;

import android.database.Cursor;
import com.android.support.lifecycle.ComputableLiveData;
import com.android.support.lifecycle.LiveData;
import com.android.support.room.CursorConverter;
import com.android.support.room.InvalidationTracker.Observer;
import com.android.support.room.Room;
import com.android.support.room.RoomDatabase;
import com.android.support.room.util.StringUtil;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.List;

public class ComplexDao_Impl extends ComplexDao {
    private final RoomDatabase __db;

    public ComplexDao_Impl(RoomDatabase __db) {
        this.__db = __db;
    }

    @Override
    public User getById(int id) {
        final String _sql = "SELECT * FROM user where uid = ?";
        final String[] _args = new String[1];
        int _argIndex = 0;
        _args[_argIndex] = Integer.toString(id);
        final Cursor _cursor = __db.query(_sql, _args);
        try {
            final CursorConverter<User> _converter = Room.getConverter(User.class);
            final User _result;
            if(_cursor.moveToFirst()) {
                _result = _converter.convert(_cursor);
            } else {
                _result = null;
            }
            return _result;
        } finally {
            _cursor.close();
        }
    }

    @Override
    public User findByName(String name, String lastName) {
        final String _sql = "SELECT * FROM user where name LIKE ? AND lastName LIKE ?";
        final String[] _args = new String[2];
        int _argIndex = 0;
        _args[_argIndex] = name;
        _argIndex = 1;
        _args[_argIndex] = lastName;
        final Cursor _cursor = __db.query(_sql, _args);
        try {
            final CursorConverter<User> _converter = Room.getConverter(User.class);
            final User _result;
            if(_cursor.moveToFirst()) {
                _result = _converter.convert(_cursor);
            } else {
                _result = null;
            }
            return _result;
        } finally {
            _cursor.close();
        }
    }

    @Override
    public List<User> loadAllByIds(int... ids) {
        StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("SELECT * FROM user where uid IN (");
        final int _inputSize = ids.length;
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final int _argCount = 0 + _inputSize;
        final String[] _args = new String[_argCount];
        int _argIndex = 0;
        for (int _item : ids) {
            _args[_argIndex] = Integer.toString(_item);
            _argIndex ++;
        }
        final Cursor _cursor = __db.query(_sql, _args);
        try {
            final CursorConverter<User> _converter = Room.getConverter(User.class);
            final List<User> _result = new ArrayList<User>(_cursor.getCount());
            while(_cursor.moveToNext()) {
                final User _item_1;
                _item_1 = _converter.convert(_cursor);
                _result.add(_item_1);
            }
            return _result;
        } finally {
            _cursor.close();
        }
    }

    @Override
    int getAge(int id) {
        final String _sql = "SELECT age FROM user where id = ?";
        final String[] _args = new String[1];
        int _argIndex = 0;
        _args[_argIndex] = Integer.toString(id);
        final Cursor _cursor = __db.query(_sql, _args);
        try {
            final int _result;
            if(_cursor.moveToFirst()) {
                _result = _cursor.getInt(0);
            } else {
                _result = 0;
            }
            return _result;
        } finally {
            _cursor.close();
        }
    }

    @Override
    public int[] getAllAges(int... ids) {
        StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("SELECT age FROM user where id = IN(");
        final int _inputSize = ids.length;
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final int _argCount = 0 + _inputSize;
        final String[] _args = new String[_argCount];
        int _argIndex = 0;
        for (int _item : ids) {
            _args[_argIndex] = Integer.toString(_item);
            _argIndex ++;
        }
        final Cursor _cursor = __db.query(_sql, _args);
        try {
            final int[] _result = new int[_cursor.getCount()];
            int _index = 0;
            while(_cursor.moveToNext()) {
                final int _item_1;
                _item_1 = _cursor.getInt(0);
                _result[_index] = _item_1;
                _index ++;
            }
            return _result;
        } finally {
            _cursor.close();
        }
    }

    @Override
    public List<Integer> getAllAgesAsList(List<Integer> ids) {
        StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("SELECT age FROM user where id = IN(");
        final int _inputSize = ids.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final int _argCount = 0 + _inputSize;
        final String[] _args = new String[_argCount];
        int _argIndex = 0;
        for (Integer _item : ids) {
            _args[_argIndex] = _item == null ? null : Integer.toString(_item);
            _argIndex ++;
        }
        final Cursor _cursor = __db.query(_sql, _args);
        try {
            final List<Integer> _result = new ArrayList<Integer>(_cursor.getCount());
            while(_cursor.moveToNext()) {
                final Integer _item_1;
                if (_cursor.isNull(0)) {
                    _item_1 = null;
                } else {
                    _item_1 = _cursor.getInt(0);
                }
                _result.add(_item_1);
            }
            return _result;
        } finally {
            _cursor.close();
        }
    }

    @Override
    public LiveData<User> getByIdLive(int id) {
        final String _sql = "SELECT * FROM user where uid = ?";
        final String[] _args = new String[1];
        int _argIndex = 0;
        _args[_argIndex] = Integer.toString(id);
        return new ComputableLiveData<User>() {
            private boolean _startedObserving = false;

            @Override
            protected User compute() {
                if (!_startedObserving) {
                    _startedObserving = true;
                    __db.getInvalidationTracker().addWeakObserver(new Observer("user") {
                        @Override
                        public void onInvalidated() {
                            invalidate();
                        }
                    });
                }
                final Cursor _cursor = __db.query(_sql, _args);
                try {
                    final CursorConverter<User> _converter = Room.getConverter(User.class);
                    final User _result;
                    if(_cursor.moveToFirst()) {
                        _result = _converter.convert(_cursor);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _cursor.close();
                }
            }
        }.getLiveData();
    }

    @Override
    public LiveData<List<User>> loadUsersByIdsLive(int... ids) {
        StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("SELECT * FROM user where uid IN (");
        final int _inputSize = ids.length;
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final int _argCount = 0 + _inputSize;
        final String[] _args = new String[_argCount];
        int _argIndex = 0;
        for (int _item : ids) {
            _args[_argIndex] = Integer.toString(_item);
            _argIndex ++;
        }
        return new ComputableLiveData<List<User>>() {
            private boolean _startedObserving = false;

            @Override
            protected List<User> compute() {
                if (!_startedObserving) {
                    _startedObserving = true;
                    __db.getInvalidationTracker().addWeakObserver(new Observer("user") {
                        @Override
                        public void onInvalidated() {
                            invalidate();
                        }
                    });
                }
                final Cursor _cursor = __db.query(_sql, _args);
                try {
                    final CursorConverter<User> _converter = Room.getConverter(User.class);
                    final List<User> _result = new ArrayList<User>(_cursor.getCount());
                    while(_cursor.moveToNext()) {
                        final User _item_1;
                        _item_1 = _converter.convert(_cursor);
                        _result.add(_item_1);
                    }
                    return _result;
                } finally {
                    _cursor.close();
                }
            }
        }.getLiveData();
    }
}
